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
	
	/**
	 * The ID of the transaction this event belongs to. Events with the same
	 * transactionId form and end-to-end chain within the application.
	 */
	public String transactionId;
	
	
	public EndpointHandler initialize() {
		this.application.initialize();
		this.location.initialize();
		this.handlingTime = null;
		this.transactionId = null;
		return this;
	}
	
	public EndpointHandler initialize(EndpointHandler copy) {
		initialize();
		if (copy == null) {
			return this;
		}
		this.application.initialize(copy.application);
		this.location.initialize(copy.location);
		this.handlingTime = copy.handlingTime;
		this.transactionId = copy.transactionId;
		return this;
	}
	
	public boolean isSet() {
		return this.handlingTime != null || this.transactionId != null || this.location.isSet() || this.application.isSet() || this.location.isSet();
	}
	
}
