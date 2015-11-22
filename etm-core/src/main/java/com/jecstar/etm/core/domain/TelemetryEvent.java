package com.jecstar.etm.core.domain;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;


public abstract class TelemetryEvent<T extends TelemetryEvent<T>> {
	
	/**
	 * The unique ID of the event.
	 */
	public String id;
	
	/**
	 * The ID of the event this event is correlated to. This is mainly used match a response to a certain request.
	 */
	public String correlationId;

	/**
	 * Data to be used for correlating event's that aren't correlated by the correlation id.
	 */
	public Map<String, String> correlationData = new HashMap<String, String>();
	
	/**
	 * The endpoint this event was send to, and received from.
	 */
	public String endpoint;
	
	/**
	 * Data to be used to query on.
	 */
	public Map<String, String> extractedData = new HashMap<String, String>();
	
	/**
	 * Metadata of the event. Not used by the application, but can be filled by the end user. 
	 */
	public Map<String, String> metadata = new HashMap<String, String>();
	
	/**
	 * The name of the event.
	 */
	public String name;

	/**
	 * The payload of the event.
	 */
	public String payload;
	
	/**
	 * The format of the payload. Generally speaking, this is a description of the {@link #payload}.
	 */
	public PayloadFormat payloadFormat;
	
	/**
	 * The ID of the transaction this event belongs to. Events with the same transactionId form and end-to-end chain in the applications landscape.
	 */
	public String transactionId;
	
	/**
	 * The handler that was writing the event.
	 */
	public EndpointHandler writingEndpointHandler = new EndpointHandler();
	
	public abstract T initialize();
	
	protected final void internalInitialize() {
		this.id = null;
		this.correlationId = null;
		this.correlationData.clear();
		this.endpoint = null;
		this.extractedData.clear();
		this.metadata.clear();
		this.payload = null;
		this.payloadFormat = null;
		this.transactionId = null;
		this.writingEndpointHandler.initialize();		
	}
	
	public abstract T initialize(T copy);
	
	protected final void internalInitialize(TelemetryEvent<?> copy) {
		this.initialize();
		if (copy == null) {
			return;
		}
		this.id = copy.id;
		this.correlationId = copy.correlationId;
		this.correlationData.putAll(copy.correlationData);
		this.endpoint = copy.endpoint;
		this.extractedData.putAll(copy.extractedData);
		this.metadata.putAll(copy.metadata);
		this.payload = copy.payload;
		this.payloadFormat = copy.payloadFormat;
		this.transactionId = copy.transactionId;
		this.writingEndpointHandler.initialize(copy.writingEndpointHandler);
	}
	
	public final ZonedDateTime getEventTime() {
		if (this.writingEndpointHandler.handlingTime != null) {
			return this.writingEndpointHandler.handlingTime;
		}
		return getInternalEventTime();
	}

	protected ZonedDateTime getInternalEventTime() {
		return ZonedDateTime.now();
	}
}
