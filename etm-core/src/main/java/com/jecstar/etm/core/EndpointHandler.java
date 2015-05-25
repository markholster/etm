package com.jecstar.etm.core;

import java.util.Date;

public class EndpointHandler {

	/**
	 * The name of the application that is handling the endpoint.
	 */
	public String applicationName;
	
	/**
	 * The time the handling took place.
	 */
	public Date handlingTime = new Date(0);
	
	public EndpointHandler initialize() {
		this.applicationName = null;
		this.handlingTime.setTime(0);
		return this;
	}
	
	public EndpointHandler initialize(EndpointHandler copy) {
		initialize();
		this.applicationName = copy.applicationName;
		this.handlingTime.setTime(copy.handlingTime.getTime());
		return this;
	}
	
}
