package com.jecstar.etm.server.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the <code>LruCache</code> class.
 */
public class LruCacheTest {

    @Test
    public void testMaxSize() {
        final int maxSize = 5;
        LruCache<Integer, Integer> cache = new LruCache<>(maxSize);
        for (int i = 0; i <= maxSize; i++) {
            cache.put(i, i);
        }
        assertEquals(maxSize, cache.size());

        // It's a LRU cache so the value with key zero should not be present anymore.
        assertFalse(cache.containsKey(0));
        // So the values 1 till maxSize + 1 should be present
        for (int i = 1; i <= maxSize; i++) {
            assertTrue(cache.containsKey(i));
        }
    }

    @Test
    public void testResize() {
        final int maxSize = 10;
        final int increaseFactor = 2;
        LruCache<Integer, Integer> cache = new LruCache<>(maxSize);
        for (int i = 0; i < maxSize * increaseFactor; i++) {
            cache.put(i, i);
        }
        assertEquals(maxSize, cache.size());

        cache.setMaxSize(maxSize * increaseFactor);
        // Increasing the cache should not clean the cache.
        assertEquals(maxSize, cache.size());
        for (int i = 0; i < maxSize * increaseFactor; i++) {
            cache.put(i, i);
        }
        // Test the new size
        assertEquals(maxSize * increaseFactor, cache.size());

        // Now resize back to maxSize
        cache.setMaxSize(maxSize);
        // Decreasing should clean the cache
        assertEquals(0, cache.size());
        // And finally the cache should not exceed the new max size
        for (int i = 0; i < maxSize * increaseFactor; i++) {
            cache.put(i, i);
        }
        assertEquals(maxSize, cache.size());
    }

    @Test
    public void testZeroSizeCache() {
        final int maxSize = 0;
        LruCache<Integer, Integer> cache = new LruCache<>(maxSize);
        for (int i = 0; i < 10; i++) {
            cache.put(i, i);
        }
        assertTrue(cache.isEmpty());
    }
}
