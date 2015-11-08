package com.jecstar.etm.gui.rest.repository;

public class Average {

	private long count;
	private float value;

	public Average(float value, long count) {
		this.count = count;
		this.value = value;
	}
	
	public float getAverage() {
		return this.value;
	}
	
	public long getCount() {
		return this.count;
	}
}
