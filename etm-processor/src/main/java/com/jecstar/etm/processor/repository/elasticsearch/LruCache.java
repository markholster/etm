package com.jecstar.etm.processor.repository.elasticsearch;

import java.util.LinkedHashMap;

public class LruCache<K,V> extends LinkedHashMap<K,V> {
	
	private static final long serialVersionUID = 4586456566155987636L;
	
	private final int maxSize;

	public LruCache(final int maxSize) {
		this.maxSize = maxSize;
	}
	
	@Override
	protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
		return size() > this.maxSize;
	}
}
