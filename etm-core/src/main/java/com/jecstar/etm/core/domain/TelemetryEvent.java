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
	public Map<String, Object> correlationData = new HashMap<String, Object>();
	
	/**
	 * The endpoint this event was send to, and received from.
	 */
	public String endpoint;
	
	/**
	 * Data to be used to query on.
	 */
	public Map<String, Object> extractedData = new HashMap<String, Object>();
	
	/**
	 * Metadata of the event. Not used by the application, but can be filled by the end user. 
	 */
	public Map<String, Object> metadata = new HashMap<String, Object>();
	
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
	 * The ID of the transaction this event belongs to. Events with the same
	 * transactionId form and end-to-end chain in the applications landscape.
	 */
	public String transactionId;
	
	/**
	 * The handler that was writing the event.
	 */
	public EndpointHandler writingEndpointHandler = new EndpointHandler();

	/**
	 * Initialize this <code>TelemetryEvent</code> with the default data. 
	 * @return A fully initialized <code>TelemetryEvent</code>.
	 */
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
	
	/**
	 * Initialize this <code>TelemetryEvent</code> with the data of another
	 * <code>TelemetryEvent</code>.
	 * 
	 * @param copy
	 *            The <code>TelemetryEvent</code> to copy the data from.
	 * @return This <code>TelemetryEvent</code> initialized with the data of the
	 *         given copy.
	 */
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
	
	/**
	 * Gives the time that applies to this <code>TelemetryEvent</code>. The
	 * value returned depends on several factors. When a writing
	 * <code>EndpointHandler<code> is provided, and it has a handling time set, 
	 * that value is returned. Otherwise by default the current time is returned. 
	 * 
	 * This method may however behave differently in subclasses. If a subclass 
	 * has one or more reading <code>EndpointHandler</code>s the handling time 
	 * of one of these handlers may be returned.
	 * 
	 * @return The event time.
	 */
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
