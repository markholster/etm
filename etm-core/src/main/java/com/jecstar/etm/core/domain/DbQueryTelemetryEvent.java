package com.jecstar.etm.core.domain;

import java.time.ZonedDateTime;

public class DbQueryTelemetryEvent extends TelemetryEvent<DbQueryTelemetryEvent>{

	public enum DbQueryEventType {
		DELETE, INSERT, SELECT, UPDATE, RESULTSET;
		
		public static DbQueryEventType saveValueOf(String value) {
			if (value == null) {
				return null;
			}
			try {
				return DbQueryEventType.valueOf(value.toUpperCase());
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
	}
	
	/**
	 * The query event type that is represented by this event.
	 */
	public DbQueryEventType dbQueryEventType;

	/**
	 * The handler that was reading the event.
	 */
	public EndpointHandler readingEndpointHandler = new EndpointHandler();
	
	@Override
	public DbQueryTelemetryEvent initialize() {
		this.dbQueryEventType = null;
		this.readingEndpointHandler.initialize();
		return this;
	}

	@Override
	public DbQueryTelemetryEvent initialize(DbQueryTelemetryEvent copy) {
		super.internalInitialize(copy);
		this.initialize();		
		this.dbQueryEventType = copy.dbQueryEventType;
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
