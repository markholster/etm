package com.jecstar.etm.core.domain;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class TelemetryEventBuilder {
	
	private TelemetryEvent event;

	public TelemetryEventBuilder() {
		this.event = new TelemetryEvent();
	}

	public TelemetryEvent build() {
		return this.event;
	}
	
	public TelemetryEventBuilder initialize() {
		this.event.initialize();
		return this;
	}
	
	public TelemetryEventBuilder setId(String id) {
		this.event.id = id;
		return this;
	}

	public TelemetryEventBuilder setCorrelationId(String correlationId) {
		this.event.correlationId = correlationId;
		return this;
	}

	public TelemetryEventBuilder setCorrelationData(Map<String, String> correlationData) {
		this.event.correlationData = correlationData;
		return this;
	}
	
	public TelemetryEventBuilder addCorrelationData(Map<String, String> correlationData) {
		this.event.correlationData.putAll(correlationData);
		return this;		
	}

	public TelemetryEventBuilder addCorrelationData(String key, String value) {
		this.event.correlationData.put(key, value);
		return this;		
	}

	public TelemetryEventBuilder setEndpoint(String endpoint) {
		this.event.endpoint = endpoint;
		return this;
	}

	public TelemetryEventBuilder setExpiry(ZonedDateTime expiry) {
		this.event.expiry = expiry;
		return this;
	}

	public TelemetryEventBuilder setExtractedData(Map<String, String> extractedData) {
		this.event.extractedData = extractedData;
		return this;
	}

	public TelemetryEventBuilder addExtractedData(Map<String, String> extractedData) {
		this.event.extractedData.putAll(extractedData);
		return this;
	}

	public TelemetryEventBuilder addExtractedData(String key, String value) {
		this.event.extractedData.put(key, value);
		return this;
	}

	public TelemetryEventBuilder setName(String name) {
		this.event.name = name;
		return this;
	}

	public TelemetryEventBuilder setMetadata(Map<String, String> metadata) {
		this.event.metadata = metadata;
		return this;
	}

	public TelemetryEventBuilder addMetadata(Map<String, String> metadata) {
		this.event.metadata.putAll(metadata);;
		return this;
	}

	public TelemetryEventBuilder addMetadata(String key, String value) {
		this.event.metadata.put(key, value);
		return this;
	}
	
	public TelemetryEventBuilder setPackaging(String packaging) {
		this.event.packaging = packaging;
		return this;
	}

	public TelemetryEventBuilder setPayload(String payload) {
		this.event.payload = payload;
		return this;
	}

	public TelemetryEventBuilder setPayloadFormat(PayloadFormat payloadFormat) {
		this.event.payloadFormat = payloadFormat;
		return this;
	}

	public TelemetryEventBuilder setTransport(Transport transport) {
		this.event.transport = transport;
		return this;
	}

	public TelemetryEventBuilder setReadingEndpointHandlers(List<EndpointHandler> readingEndpointHandlers) {
		this.event.readingEndpointHandlers = readingEndpointHandlers;
		return this;
	}

	public TelemetryEventBuilder addReadingEndpointHandlers(List<EndpointHandler> readingEndpointHandlers) {
		this.event.readingEndpointHandlers.addAll(readingEndpointHandlers);
		return this;
	}

	public TelemetryEventBuilder addReadingEndpointHandler(EndpointHandler readingEndpointHandler) {
		this.event.readingEndpointHandlers.add(readingEndpointHandler);
		return this;
	}
	
	public TelemetryEventBuilder addReadingEndpointHandler(ZonedDateTime handlingTime, String applicationName, String applicationVersion, String applicationInstance, String principal) {
		EndpointHandler endpointHandler = new EndpointHandler();
		endpointHandler.handlingTime = handlingTime;
		endpointHandler.application.name = applicationName;
		endpointHandler.application.version = applicationVersion;
		endpointHandler.application.instance = applicationInstance;
		endpointHandler.application.principal = principal;
		addReadingEndpointHandler(endpointHandler);
		return this;
	}

	public TelemetryEventBuilder setWritingEndpointHandler(EndpointHandler writingEndpointHandler) {
		this.event.writingEndpointHandler = writingEndpointHandler;
		return this;
	}
	
	public TelemetryEventBuilder setWritingEndpointHandler(ZonedDateTime handlingTime, String applicationName, String applicationVersion, String applicationInstance, String principal) {
		this.event.writingEndpointHandler.handlingTime = handlingTime;
		this.event.writingEndpointHandler.application.name = applicationName;
		this.event.writingEndpointHandler.application.version = applicationVersion;
		this.event.writingEndpointHandler.application.instance = applicationInstance;
		this.event.writingEndpointHandler.application.principal = principal;
		return this;
	}
}
