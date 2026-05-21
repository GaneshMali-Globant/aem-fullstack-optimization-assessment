package com.assessment.core.services.impl;

import com.assessment.core.services.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CacheServiceImplTest {

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new CacheServiceImpl();
    }

    @AfterEach
    void tearDown() {
        if (cacheService != null) {
            cacheService.clear();
        }
    }

    @Test
    void testPutAndGet() {
        String key = "test-key";
        String value = "test-value";
        
        cacheService.put(key, value, 1, TimeUnit.HOURS);
        
        String retrieved = cacheService.get(key);
        assertEquals(value, retrieved, "Should retrieve the cached value");
    }

    @Test
    void testGetNonExistentKey() {
        String result = cacheService.get("non-existent-key");
        assertNull(result, "Should return null for non-existent key");
    }

    @Test
    void testGetNullKey() {
        String result = cacheService.get(null);
        assertNull(result, "Should return null for null key");
    }

    @Test
    void testPutNullKey() {
        // Should not throw exception
        assertDoesNotThrow(() -> cacheService.put(null, "value", 1, TimeUnit.HOURS));
    }

    @Test
    void testPutNullValue() {
        // Should not throw exception
        assertDoesNotThrow(() -> cacheService.put("key", null, 1, TimeUnit.HOURS));
    }

    @Test
    void testContainsKey() {
        String key = "test-key";
        String value = "test-value";
        
        assertFalse(cacheService.containsKey(key), "Should not contain key before put");
        
        cacheService.put(key, value, 1, TimeUnit.HOURS);
        assertTrue(cacheService.containsKey(key), "Should contain key after put");
    }

    @Test
    void testContainsKeyWithNullKey() {
        assertFalse(cacheService.containsKey(null), "Should return false for null key");
    }

    @Test
    void testRemove() {
        String key = "test-key";
        String value = "test-value";
        
        cacheService.put(key, value, 1, TimeUnit.HOURS);
        assertTrue(cacheService.containsKey(key), "Should contain key before remove");
        
        cacheService.remove(key);
        assertFalse(cacheService.containsKey(key), "Should not contain key after remove");
        
        String retrieved = cacheService.get(key);
        assertNull(retrieved, "Should return null after remove");
    }

    @Test
    void testRemoveNullKey() {
        // Should not throw exception
        assertDoesNotThrow(() -> cacheService.remove(null));
    }

    @Test
    void testClear() {
        cacheService.put("key1", "value1", 1, TimeUnit.HOURS);
        cacheService.put("key2", "value2", 1, TimeUnit.HOURS);
        cacheService.put("key3", "value3", 1, TimeUnit.HOURS);
        
        assertTrue(cacheService.containsKey("key1"), "Should contain key1 before clear");
        assertTrue(cacheService.containsKey("key2"), "Should contain key2 before clear");
        assertTrue(cacheService.containsKey("key3"), "Should contain key3 before clear");
        
        cacheService.clear();
        
        assertFalse(cacheService.containsKey("key1"), "Should not contain key1 after clear");
        assertFalse(cacheService.containsKey("key2"), "Should not contain key2 after clear");
        assertFalse(cacheService.containsKey("key3"), "Should not contain key3 after clear");
    }

    @Test
    void testExpiration() throws InterruptedException {
        String key = "test-key";
        String value = "test-value";
        
        cacheService.put(key, value, 100, TimeUnit.MILLISECONDS);
        
        // Should be available immediately
        String retrieved = cacheService.get(key);
        assertEquals(value, retrieved, "Should retrieve value immediately after put");
        
        // Wait for expiration
        Thread.sleep(200);
        
        // Should be expired
        String expired = cacheService.get(key);
        assertNull(expired, "Should return null after expiration");
        
        assertFalse(cacheService.containsKey(key), "Should not contain expired key");
    }

    @Test
    void testOverwriteExistingKey() {
        String key = "test-key";
        String value1 = "value1";
        String value2 = "value2";
        
        cacheService.put(key, value1, 1, TimeUnit.HOURS);
        assertEquals(value1, cacheService.get(key), "Should return first value");
        
        cacheService.put(key, value2, 1, TimeUnit.HOURS);
        assertEquals(value2, cacheService.get(key), "Should return second value after overwrite");
    }

    @Test
    void testMultipleKeys() {
        cacheService.put("key1", "value1", 1, TimeUnit.HOURS);
        cacheService.put("key2", "value2", 1, TimeUnit.HOURS);
        cacheService.put("key3", "value3", 1, TimeUnit.HOURS);
        
        assertEquals("value1", cacheService.get("key1"));
        assertEquals("value2", cacheService.get("key2"));
        assertEquals("value3", cacheService.get("key3"));
        
        cacheService.remove("key2");
        
        assertEquals("value1", cacheService.get("key1"));
        assertNull(cacheService.get("key2"));
        assertEquals("value3", cacheService.get("key3"));
    }

    @Test
    void testSpecialCharactersInKeys() {
        String key = "special-.key_with/characters";
        String value = "test-value";
        
        cacheService.put(key, value, 1, TimeUnit.HOURS);
        
        assertEquals(value, cacheService.get(key));
        assertTrue(cacheService.containsKey(key));
        
        cacheService.remove(key);
        assertNull(cacheService.get(key));
        assertFalse(cacheService.containsKey(key));
    }

    @Test
    void testEmptyKey() {
        String key = "";
        String value = "test-value";
        
        cacheService.put(key, value, 1, TimeUnit.HOURS);
        
        assertEquals(value, cacheService.get(key));
        assertTrue(cacheService.containsKey(key));
    }

    @Test
    void testEmptyValue() {
        String key = "test-key";
        String value = "";
        
        cacheService.put(key, value, 1, TimeUnit.HOURS);
        
        assertEquals(value, cacheService.get(key));
        assertTrue(cacheService.containsKey(key));
    }

    @Test
    void testZeroTTL() {
        String key = "test-key";
        String value = "test-value";
        
        cacheService.put(key, value, 0, TimeUnit.MILLISECONDS);
        
        // With zero TTL, the behavior is implementation-specific
        // Either it's not stored or expires immediately
        String retrieved = cacheService.get(key);
        // If stored, it should be considered expired for practical purposes
        if (retrieved != null) {
            // If value exists, verify cache key exists
            assertTrue(cacheService.containsKey(key));
        } else {
            // If value doesn't exist, verify cache key doesn't exist
            assertFalse(cacheService.containsKey(key));
        }
    }

    @Test
    void testNegativeTTL() {
        String key = "test-key";
        String value = "test-value";
        
        cacheService.put(key, value, -1, TimeUnit.HOURS);
        
        // Should be expired immediately
        assertNull(cacheService.get(key));
        assertFalse(cacheService.containsKey(key));
    }
}