package com.jecstar.etm.server.core.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class LruCache<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 4586456566155987636L;
    private final Map<K, Long> expiryMap;
    private final long expiry;

    private int maxSize;

    public LruCache(final int maxSize) {
        this(maxSize, -1);
    }

    public LruCache(final int maxSize, final long expiry) {
        this.maxSize = maxSize;
        this.expiry = expiry;
        if (this.expiry > -1) {
            this.expiryMap = new LinkedHashMap<K, Long>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > LruCache.this.maxSize;
                }
            };
        } else {
            this.expiryMap = null;
        }
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

    @Override
    public void clear() {
        if (this.expiryMap != null) {
            this.expiryMap.clear();
        }
        super.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        removeWhenExpired(key);
        return super.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (this.expiryMap != null) {
            throw new UnsupportedOperationException();
        }
        return super.containsValue(value);
    }

    @Override
    public V put(K key, V value) {
        if (this.expiryMap != null) {
            this.expiryMap.put(key, System.currentTimeMillis());
        }
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (this.expiryMap != null) {
            long time = System.currentTimeMillis();
            m.forEach((k, v) -> this.expiryMap.put(k, time));
        }
        super.putAll(m);
    }

    @Override
    public V get(Object key) {
        removeWhenExpired(key);
        return super.get(key);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        removeWhenExpired(key);
        return super.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean remove(Object key, Object value) {
        boolean removed = super.remove(key, value);
        if (removed && this.expiryMap != null) {
            this.expiryMap.remove(key);
        }
        return removed;
    }

    @Override
    public V remove(Object key) {
        if (this.expiryMap != null) {
            this.expiryMap.remove(key);
        }
        return super.remove(key);
    }

    @Override
    public V replace(K key, V value) {
        if (this.expiryMap != null) {
            this.expiryMap.replace(key, System.currentTimeMillis());
        }
        return super.replace(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        boolean replaced = super.replace(key, oldValue, newValue);
        if (replaced && this.expiryMap != null) {
            this.expiryMap.replace(key, System.currentTimeMillis());
        }
        return replaced;
    }

    @Override
    public int size() {
        removeAllExpired();
        return super.size();
    }

    @Override
    public boolean isEmpty() {
        removeAllExpired();
        return super.isEmpty();
    }

    private void removeWhenExpired(Object key) {
        if (this.expiryMap != null) {
            Long insertAt = this.expiryMap.get(key);
            if (insertAt != null && System.currentTimeMillis() - insertAt > this.expiry) {
                remove(key);
            }
        }
    }

    private void removeAllExpired() {
        if (this.expiryMap != null) {
            Iterator<Map.Entry<K, V>> iterator = entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<K, V> entry = iterator.next();
                Long insertAt = this.expiryMap.get(entry.getKey());
                if (insertAt != null && System.currentTimeMillis() - insertAt > this.expiry) {
                    iterator.remove();
                    this.expiryMap.remove(entry.getKey());
                }
            }
        }
    }

}
