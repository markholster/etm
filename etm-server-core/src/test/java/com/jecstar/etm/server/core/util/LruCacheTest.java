/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.server.core.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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


    @Test
    public void testCacheExpiryWithSizeMethod() {
        final int maxSize = 100;
        final int expiry = 10;
        LruCache<Integer, Integer> cache = new LruCache<>(maxSize, expiry);
        for (int i = 0; i < maxSize; i++) {
            cache.put(i, i);
        }
        try {
            Thread.sleep(expiry + 1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertEquals(0, cache.size());
    }

    @Test
    public void testCacheExpiryWithEmptyMethod() {
        final int maxSize = 100;
        final int expiry = 10;
        LruCache<Integer, Integer> cache = new LruCache<>(maxSize, expiry);
        for (int i = 0; i < maxSize; i++) {
            cache.put(i, i);
        }
        try {
            Thread.sleep(expiry + 1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testCacheExpiryWithGetMethod() {
        final int maxSize = 1;
        final int expiry = 1000;
        final Integer key = 200;
        LruCache<Integer, Integer> cache = new LruCache<>(maxSize, expiry);
        cache.put(key, 1);
        assertNotNull(cache.get(key));
        try {
            Thread.sleep(expiry + 1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertNull(cache.get(key));
    }


    @Test
    public void testCacheSizeWithExpiry() {
        final int maxSize = 1;
        final int expiry = 100_000;
        LruCache<Integer, Integer> cache = new LruCache<>(maxSize, expiry);
        for (int i = 0; i < 10; i++) {
            cache.put(i, i);
        }
        assertEquals(maxSize, cache.size());
    }

    @Test
    public void testMaxSizeMultiThreads() {
        final int maxSize = 5000;
        final int expiry = 5000;
        final int nrOfThreads = 5;
        LruCache<Integer, Integer> cache = new LruCache<>(maxSize, expiry);

        ExecutorService executorService = Executors.newFixedThreadPool(nrOfThreads);
        for (int i = 0; i < nrOfThreads; i++) {
            final int instanceIx = i;
            executorService.submit(() -> {
                for (int j = 0; j < 500_000; j++) {
                    Integer uniqueValue = Integer.valueOf("" + instanceIx + j);
                    cache.put(uniqueValue, uniqueValue);
                    cache.get(uniqueValue);
                    cache.get(uniqueValue);
                    cache.put(uniqueValue, uniqueValue);
                    cache.get(uniqueValue);
                    cache.put(uniqueValue, uniqueValue);
                    cache.get(uniqueValue);
                    cache.put(uniqueValue, uniqueValue);
                    cache.put(uniqueValue, uniqueValue);
                }
            });
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e);
        }
        assertEquals(maxSize, cache.size());

        try {
            Thread.sleep(expiry);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e);
        }
        // After the expiry period the cache should be empty.
        assertTrue(cache.isEmpty());
    }
}
