package com.jecstar.etm.core.domain;

import java.time.LocalDateTime;

public class EndpointHandler {

	/**
	 * The <code>Application that is handling the endpoint.
	 */
	public Application application = new Application();
	
	/**
	 * The time the handling took place.
	 */
	public LocalDateTime handlingTime;
	
	
	public EndpointHandler initialize() {
		this.application.initialize();
		this.handlingTime = null;
		return this;
	}
	
	public EndpointHandler initialize(EndpointHandler copy) {
		initialize();
		this.application.initialize(copy.application);
		this.handlingTime = copy.handlingTime;
		return this;
	}
	
}
