package com.jecstar.etm.domain.builders;

import com.jecstar.etm.domain.Location;

public class LocationBuilder {

	private final Location location;
	
	public LocationBuilder() {
		this.location = new Location();
	}
	
	public Location build() {
		return this.location;
	}
	
	public LocationBuilder setLatitude(Double latitude) {
		this.location.latitude = latitude;
		return this;
	}
	
	public LocationBuilder setLongitude(Double longitude) {
		this.location.longitude = longitude;
		return this;
	}
}
