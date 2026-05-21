package com.assessment.core.services.impl;

import com.assessment.core.services.CacheService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component(service = CacheService.class, immediate = true)
public class CacheServiceImpl implements CacheService {
    
    private static final Logger LOG = LoggerFactory.getLogger(CacheServiceImpl.class);
    
    private static class CacheEntry {
        private final String value;
        private final long expirationTime;
        
        public CacheEntry(String value, long ttlMillis) {
            this.value = value;
            this.expirationTime = System.currentTimeMillis() + ttlMillis;
        }
        
        public String getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
    
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    public CacheServiceImpl() {
        // Schedule cleanup task to run every hour
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 1, 1, TimeUnit.HOURS);
    }
    
    @Override
    public String get(String key) {
        if (key == null) {
            return null;
        }
        
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(key);
            if (entry != null && !entry.isExpired()) {
                LOG.debug("Cache hit for key: {}", key);
                return entry.getValue();
            }
            
            // Remove expired entry
            if (entry != null && entry.isExpired()) {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    cache.remove(key);
                    LOG.debug("Removed expired entry for key: {}", key);
                } finally {
                    lock.writeLock().unlock();
                    lock.readLock().lock();
                }
            }
            
            LOG.debug("Cache miss for key: {}", key);
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void put(String key, String value, long ttl, TimeUnit timeUnit) {
        if (key == null || value == null) {
            LOG.warn("Attempted to cache null key or value");
            return;
        }
        
        lock.writeLock().lock();
        try {
            CacheEntry entry = new CacheEntry(value, timeUnit.toMillis(ttl));
            cache.put(key, entry);
            LOG.debug("Cached value for key: {} with TTL: {} {}", key, ttl, timeUnit);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void remove(String key) {
        if (key == null) {
            return;
        }
        
        lock.writeLock().lock();
        try {
            cache.remove(key);
            LOG.debug("Removed cache entry for key: {}", key);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
            LOG.debug("Cleared all cache entries");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean containsKey(String key) {
        if (key == null) {
            return false;
        }
        
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(key);
            return entry != null && !entry.isExpired();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private void cleanupExpiredEntries() {
        lock.writeLock().lock();
        try {
            int removedCount = 0;
            for (String key : cache.keySet()) {
                CacheEntry entry = cache.get(key);
                if (entry != null && entry.isExpired()) {
                    cache.remove(key);
                    removedCount++;
                }
            }
            if (removedCount > 0) {
                LOG.info("Cleaned up {} expired cache entries", removedCount);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @SuppressWarnings("deprecation")
    protected void deactivate() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}