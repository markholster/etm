package com.jecstar.etm.domain;

import com.jecstar.etm.domain.HttpTelemetryEvent.HttpEventType;

public class HttpTelemetryEventBuilder extends TelemetryEventBuilder<HttpTelemetryEvent, HttpTelemetryEventBuilder> {

	public HttpTelemetryEventBuilder() {
		super(new HttpTelemetryEvent());
	}
	
	public HttpTelemetryEventBuilder setHttpEventType(HttpEventType httpEventType) {
		this.event.httpEventType = httpEventType;
		return this;
	}
}
