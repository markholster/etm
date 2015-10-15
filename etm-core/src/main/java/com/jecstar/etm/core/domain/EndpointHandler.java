package com.jecstar.etm.core.domain;

import java.time.ZonedDateTime;

public class EndpointHandler {

	/**
	 * The <code>Application</code> that is handling the endpoint.
	 */
	public Application application = new Application();
	
	/**
	 * The <code>Location</code> the handling took place.
	 */
	public Location location = new Location();
	
	/**
	 * The time the handling took place.
	 */
	public ZonedDateTime handlingTime;
	
	
	public EndpointHandler initialize() {
		this.application.initialize();
		this.handlingTime = null;
		return this;
	}
	
	public EndpointHandler initialize(EndpointHandler copy) {
		initialize();
		this.application.initialize(copy.application);
		this.location.initialize(copy.location);
		this.handlingTime = copy.handlingTime;
		return this;
	}
	
	public boolean isSet() {
		if (this.handlingTime == null) {
			return false;
		}
		return this.application.isSet() || this.location.isSet();
	}
	
}
