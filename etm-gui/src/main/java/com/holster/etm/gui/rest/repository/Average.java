package com.holster.etm.gui.rest.repository;

public class Average {

	private long count;
	private long value;

	public Average(long value) {
		this.count = 1;
		this.value = value;
	}
	public void add(long value) {
		this.count++;
		this.value += value;
	}
	
	public float getAverage() {
		return (float)this.value / (float)this.count;
	}
	
	public long getCount() {
		return this.count;
	}
}
