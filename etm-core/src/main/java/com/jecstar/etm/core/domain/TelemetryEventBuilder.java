package com.jecstar.etm.core.domain;

import java.util.Map;

public abstract class TelemetryEventBuilder<Event extends TelemetryEvent<Event>, Builder extends TelemetryEventBuilder<Event, Builder>> {
	
	protected Event event;

	protected TelemetryEventBuilder(Event event) {
		this.event = event;
	}

	public Event build() {
		return this.event;
	}
	
	@SuppressWarnings("unchecked")
	public Builder initialize() {
		this.event.initialize();
		return (Builder) this;
	}
	
	@SuppressWarnings("unchecked")
	public Builder setId(String id) {
		this.event.id = id;
		return (Builder) this;
	}

	@SuppressWarnings("unchecked")
	public Builder setCorrelationId(String correlationId) {
		this.event.correlationId = correlationId;
		return (Builder) this;
	}

	@SuppressWarnings("unchecked")
	public Builder setCorrelationData(Map<String, Object> correlationData) {
		this.event.correlationData = correlationData;
		return (Builder) this;
	}
	
	@SuppressWarnings("unchecked")
	public Builder addCorrelationData(Map<String, Object> correlationData) {
		this.event.correlationData.putAll(correlationData);
		return (Builder) this;		
	}

	@SuppressWarnings("unchecked")
	public Builder addCorrelationData(String key, Object value) {
		this.event.correlationData.put(key, value);
		return (Builder) this;		
	}

	@SuppressWarnings("unchecked")
	public Builder addOrMergeEndpoint(Endpoint endpoint) {
		int ix = this.event.endpoints.indexOf(endpoint);
		if (ix == -1) {
			this.event.endpoints.add(endpoint);
		} else {
			Endpoint currentEndpoint = this.event.endpoints.get(ix);
			if (!endpoint.writingEndpointHandler.isSet()) {
				currentEndpoint.writingEndpointHandler.initialize(endpoint.writingEndpointHandler);
			}
			for (EndpointHandler handler : endpoint.readingEndpointHandlers) {
				if (handler.isSet()) {
					currentEndpoint.readingEndpointHandlers.add(handler);
				}
			}
		}
		return (Builder) this;
	}
	
	public Builder addOrMergeEndpoint(EndpointBuilder endpointBuilder) {
		return addOrMergeEndpoint(endpointBuilder.build());
	}

	@SuppressWarnings("unchecked")
	public Builder setExtractedData(Map<String, Object> extractedData) {
		this.event.extractedData = extractedData;
		return (Builder) this;
	}

	@SuppressWarnings("unchecked")
	public Builder addExtractedData(Map<String, Object> extractedData) {
		this.event.extractedData.putAll(extractedData);
		return (Builder) this;
	}

	@SuppressWarnings("unchecked")
	public Builder addExtractedData(String key, Object value) {
		this.event.extractedData.put(key, value);
		return (Builder) this;
	}

	@SuppressWarnings("unchecked")
	public Builder setName(String name) {
		this.event.name = name;
		return (Builder) this;
	}

	@SuppressWarnings("unchecked")
	public Builder setMetadata(Map<String, Object> metadata) {
		this.event.metadata = metadata;
		return (Builder) this;
	}

	@SuppressWarnings("unchecked")
	public Builder addMetadata(Map<String, Object> metadata) {
		this.event.metadata.putAll(metadata);;
		return (Builder) this;
	}

	@SuppressWarnings("unchecked")
	public Builder addMetadata(String key, Object value) {
		this.event.metadata.put(key, value);
		return (Builder) this;
	}

	@SuppressWarnings("unchecked")
	public Builder setPayload(String payload) {
		this.event.payload = payload;
		return (Builder) this;
	}

	@SuppressWarnings("unchecked")
	public Builder setPayloadFormat(PayloadFormat payloadFormat) {
		this.event.payloadFormat = payloadFormat;
		return (Builder) this;
	}
}
