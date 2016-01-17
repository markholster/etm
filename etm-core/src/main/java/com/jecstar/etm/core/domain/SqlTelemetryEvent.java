package com.jecstar.etm.core.domain;

import java.time.ZonedDateTime;

public class SqlTelemetryEvent extends TelemetryEvent<SqlTelemetryEvent>{

	public enum SqlEventType {
		DELETE, INSERT, SELECT, UPDATE, RESULTSET;
		
		public static SqlEventType safeValueOf(String value) {
			if (value == null) {
				return null;
			}
			try {
				return SqlEventType.valueOf(value.toUpperCase());
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
	}
	
	/**
	 * The query event type that is represented by this event.
	 */
	public SqlEventType sqlEventType;

	/**
	 * The handler that was reading the event.
	 */
	public EndpointHandler readingEndpointHandler = new EndpointHandler();
	
	@Override
	public SqlTelemetryEvent initialize() {
		this.sqlEventType = null;
		this.readingEndpointHandler.initialize();
		return this;
	}

	@Override
	public SqlTelemetryEvent initialize(SqlTelemetryEvent copy) {
		super.internalInitialize(copy);
		this.sqlEventType = copy.sqlEventType;
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
