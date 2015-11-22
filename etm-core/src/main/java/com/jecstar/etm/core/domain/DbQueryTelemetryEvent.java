package com.jecstar.etm.core.domain;

import java.time.ZonedDateTime;

public class DbQueryTelemetryEvent extends TelemetryEvent<DbQueryTelemetryEvent>{

	public enum QueryEventType {DELETE, INSERT, SELECT, UPDATE, RESULTSET}
	
	/**
	 * The query event type that is represented by this event.
	 */
	public QueryEventType queryEventType;

	/**
	 * The handler that was reading the event.
	 */
	public EndpointHandler readingEndpointHandler = new EndpointHandler();
	
	@Override
	public DbQueryTelemetryEvent initialize() {
		this.queryEventType = null;
		this.readingEndpointHandler.initialize();
		return this;
	}

	@Override
	public DbQueryTelemetryEvent initialize(DbQueryTelemetryEvent copy) {
		super.internalInitialize(copy);
		this.initialize();		
		this.queryEventType = copy.queryEventType;
		this.readingEndpointHandler.initialize(copy.readingEndpointHandler);
		return this;
	}
	
	@Override
	protected ZonedDateTime getInternalEventTime() {
		if (this.readingEndpointHandler.handlingTime != null) {
			return this.readingEndpointHandler.handlingTime;
		}
		return super.getInternalEventTime();
	}
}
