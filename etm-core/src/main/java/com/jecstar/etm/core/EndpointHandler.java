package com.jecstar.etm.core;

import java.time.LocalDateTime;

public class EndpointHandler {

	/**
	 * The name of the application that is handling the endpoint.
	 */
	public String applicationName;
	
	/**
	 * The time the handling took place.
	 */
	public LocalDateTime handlingTime;
	
	public EndpointHandler initialize() {
		this.applicationName = null;
		this.handlingTime = null;
		return this;
	}
	
	public EndpointHandler initialize(EndpointHandler copy) {
		initialize();
		this.applicationName = copy.applicationName;
		this.handlingTime = copy.handlingTime;
		return this;
	}
	
}
