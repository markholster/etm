package com.jecstar.etm.core.domain;

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

	@Override
	public HttpTelemetryEvent initialize() {
		super.internalInitialize();
		this.httpEventType = null;
		return this;
	}

	@Override
	public HttpTelemetryEvent initialize(HttpTelemetryEvent copy) {
		super.internalInitialize(copy);
		this.httpEventType = copy.httpEventType;
		return this;
	}

}
