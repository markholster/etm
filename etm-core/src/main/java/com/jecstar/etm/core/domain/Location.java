package com.jecstar.etm.core.domain;

public class Location {

	/**
	 * The latitude part of the location.
	 */
	public Double latitude;
	
	/**
	 * The longitude part of the location.
	 */
	public Double longitude;
	
	public Location initialize() {
		this.latitude = null;
		this.longitude = null;
		return this;
	}
	
	public Location initialize(Location copy) {
		this.initialize();
		this.latitude = copy.latitude;
		this.longitude = copy.longitude;
		return this;
	}
	
	public boolean isSet() {
		return this.latitude != null && this.longitude != null;
	}
}
