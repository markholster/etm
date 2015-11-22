package com.jecstar.etm.core.domain;

import java.time.ZonedDateTime;
import java.util.List;

import com.jecstar.etm.core.domain.MessagingTelemetryEvent.MessagingEventType;

public class MessagingTelemetryEventBuilder extends TelemetryEventBuilder<MessagingTelemetryEvent, MessagingTelemetryEventBuilder> {

	public MessagingTelemetryEventBuilder() {
		super(new MessagingTelemetryEvent());
	}

	public MessagingTelemetryEventBuilder setExpiry(ZonedDateTime expiry) {
		this.event.expiry = expiry;
		return this;
	}
	
	public MessagingTelemetryEventBuilder setMessagingEventType(MessagingEventType messagingEventType) {
		this.event.messagingEventType = messagingEventType;
		return this;
	}
	
	public MessagingTelemetryEventBuilder setReadingEndpointHandlers(List<EndpointHandler> readingEndpointHandlers) {
		this.event.readingEndpointHandlers = readingEndpointHandlers;
		return this;
	}
	
	public MessagingTelemetryEventBuilder addReadingEndpointHandlers(List<EndpointHandler> readingEndpointHandlers) {
		this.event.readingEndpointHandlers.addAll(readingEndpointHandlers);
		return this;
	}
	
	public MessagingTelemetryEventBuilder addReadingEndpointHandler(EndpointHandler readingEndpointHandler) {
		this.event.readingEndpointHandlers.add(readingEndpointHandler);
		return this;
	}
	
	public MessagingTelemetryEventBuilder addReadingEndpointHandler(ZonedDateTime handlingTime, String applicationName, String applicationVersion, String applicationInstance, String principal) {
		EndpointHandler endpointHandler = new EndpointHandler();
		endpointHandler.handlingTime = handlingTime;
		endpointHandler.application.name = applicationName;
		endpointHandler.application.version = applicationVersion;
		endpointHandler.application.instance = applicationInstance;
		endpointHandler.application.principal = principal;
		addReadingEndpointHandler(endpointHandler);
		return this;
	}
}
