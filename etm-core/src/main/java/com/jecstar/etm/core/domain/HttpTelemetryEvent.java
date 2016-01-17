package com.jecstar.etm.core.domain;

import java.time.ZonedDateTime;

public class HttpTelemetryEvent extends TelemetryEvent<HttpTelemetryEvent> {
	
	public enum HttpEventType {
		CONNECT, DELETE, GET, HEAD, OPTIONS, POST, PUT, TRACE, RESPONSE;
		
		public static HttpEventType safeValueOf(String value) {
			if (value == null) {
				return null;
			}
			try {
				return HttpEventType.valueOf(value.toUpperCase());
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
	}

	/**
	 * The http type that is represented by this event.
	 */
	public HttpEventType httpEventType;

	/**
	 * The handler that was reading the event.
	 */
	public EndpointHandler readingEndpointHandler = new EndpointHandler();

	
	@Override
	public HttpTelemetryEvent initialize() {
		super.internalInitialize();
		this.httpEventType = null;
		this.readingEndpointHandler.initialize();
		return this;
	}

	@Override
	public HttpTelemetryEvent initialize(HttpTelemetryEvent copy) {
		super.internalInitialize(copy);
		this.httpEventType = copy.httpEventType;
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
