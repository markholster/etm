package com.jecstar.etm.domain;

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
	 * The moment this event expires, in case of a request. This is the time the
	 * even occurred added with the request timeout. This value should usually
	 * be calculated by the client that is sending the http request.
	 */
	public ZonedDateTime expiry;


	@Override
	public HttpTelemetryEvent initialize() {
		super.internalInitialize();
		this.expiry = null;
		this.httpEventType = null;
		return this;
	}

	@Override
	public HttpTelemetryEvent initialize(HttpTelemetryEvent copy) {
		super.internalInitialize(copy);
		this.expiry = copy.expiry;
		this.httpEventType = copy.httpEventType;
		return this;
	}

}
