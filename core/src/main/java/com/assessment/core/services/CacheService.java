package com.assessment.core.services;

import java.util.concurrent.TimeUnit;

public interface CacheService {
    
    /**
     * Get cached value for the given key
     * @param key the cache key
     * @return cached value or null if not found or expired
     */
    String get(String key);
    
    /**
     * Put value in cache with specified TTL
     * @param key the cache key
     * @param value the value to cache
     * @param ttl time to live
     * @param timeUnit time unit for TTL
     */
    void put(String key, String value, long ttl, TimeUnit timeUnit);
    
    /**
     * Remove cached value for the given key
     * @param key the cache key
     */
    void remove(String key);
    
    /**
     * Clear all cache entries
     */
    void clear();
    
    /**
     * Check if key exists in cache and is not expired
     * @param key the cache key
     * @return true if key exists and is valid
     */
    boolean containsKey(String key);
}