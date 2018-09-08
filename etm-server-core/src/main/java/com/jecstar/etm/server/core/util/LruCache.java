package com.jecstar.etm.server.core.util;

import java.util.LinkedHashMap;

public class LruCache<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 4586456566155987636L;

    private int maxSize;

    public LruCache(final int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
        return size() > this.maxSize;
    }

    /**
     * Sets the maximum size of the cache. If the new size is smaller than the current size the cache will be cleared.
     *
     * @param maxSize The maximum size of the cache.
     */
    public void setMaxSize(final int maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException();
        }
        boolean needToEmptyCache = maxSize < this.maxSize;
        this.maxSize = maxSize;
        if (needToEmptyCache) {
            clear();
        }
    }
}
