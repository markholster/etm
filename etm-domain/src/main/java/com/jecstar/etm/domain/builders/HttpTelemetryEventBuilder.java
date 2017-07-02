package com.jecstar.etm.domain.builders;

import java.time.ZonedDateTime;

import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.HttpTelemetryEvent.HttpEventType;

public class HttpTelemetryEventBuilder extends TelemetryEventBuilder<HttpTelemetryEvent, HttpTelemetryEventBuilder> {

	public HttpTelemetryEventBuilder() {
		super(new HttpTelemetryEvent());
	}
	
	public HttpTelemetryEventBuilder setExpiry(ZonedDateTime expiry) {
		this.event.expiry = expiry;
		return this;
	}

	
	public HttpTelemetryEventBuilder setHttpEventType(HttpEventType httpEventType) {
		this.event.httpEventType = httpEventType;
		return this;
	}
	
	public HttpEventType getHttpEventType() {
		return this.event.httpEventType;
	}
}
