package com.jecstar.etm.core.domain;

import java.time.ZonedDateTime;

import com.jecstar.etm.core.domain.HttpTelemetryEvent.HttpEventType;

public class HttpTelemetryEventBuilder extends TelemetryEventBuilder<HttpTelemetryEvent, HttpTelemetryEventBuilder> {

	public HttpTelemetryEventBuilder() {
		super(new HttpTelemetryEvent());
	}
	
	public HttpTelemetryEventBuilder setHttpEventType(HttpEventType httpEventType) {
		this.event.httpEventType = httpEventType;
		return this;
	}
	
	public HttpTelemetryEventBuilder setReadingEndpointHandler(EndpointHandler writingEndpointHandler) {
		this.event.readingEndpointHandler = writingEndpointHandler;
		return this;
	}
	
	public HttpTelemetryEventBuilder setReadingEndpointHandler(ZonedDateTime handlingTime, String applicationName, String applicationVersion, String applicationInstance, String principal) {
		this.event.readingEndpointHandler.handlingTime = handlingTime;
		this.event.readingEndpointHandler.application.name = applicationName;
		this.event.readingEndpointHandler.application.version = applicationVersion;
		this.event.readingEndpointHandler.application.instance = applicationInstance;
		this.event.readingEndpointHandler.application.principal = principal;
		return this;
	}

}
