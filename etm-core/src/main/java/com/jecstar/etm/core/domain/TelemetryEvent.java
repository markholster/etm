package com.jecstar.etm.core.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TelemetryEvent {

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
	 * The name of the event.
	 */
	public String name;
	
	/**
	 * Metadata of the event. Not used by the application, but can be filled by the end user. 
	 */
	public Map<String, String> metadata = new HashMap<String, String>();

	/**
	 * The payload of the event.
	 */
	public String payload;
	
	/**
	 * The type of this <code>TelemetryEvent</code>. Generally speaking, this is a description of the {@link #payload}.
	 */
	public TelemetryEventType telemetryEventType;
	
	/**
	 * The transport type used to send the {@link #payload} to from the sending application to the receiving application(s).
	 */
	public TransportType transportType;

	/**
	 * The handlers that were reading the event.
	 */
	public List<EndpointHandler> readingEndpointHandlers = new ArrayList<EndpointHandler>();
	
	/**
	 * The handler that was writing the event.
	 */
	public EndpointHandler writingEndpointHandler = new EndpointHandler();
	
	public TelemetryEvent initialize() {
		this.id = null;
		this.correlationId = null;
		this.correlationData.clear();
		this.metadata.clear();
		this.payload = null;
		this.readingEndpointHandlers.clear();
		this.telemetryEventType = null;
		this.transportType = null;
		this.writingEndpointHandler.initialize();
		return this;
	}
	
	public TelemetryEvent initialize(TelemetryEvent copy) {
		this.initialize();
		this.id = copy.id;
		this.correlationId = copy.correlationId;
		this.correlationData.putAll(copy.correlationData);
		this.metadata.putAll(copy.metadata);
		this.payload = copy.payload;
		this.readingEndpointHandlers.addAll(copy.readingEndpointHandlers);
		this.telemetryEventType = copy.telemetryEventType;
		this.transportType = copy.transportType;
		this.writingEndpointHandler.initialize(copy.writingEndpointHandler);
		return this;
	}


}
