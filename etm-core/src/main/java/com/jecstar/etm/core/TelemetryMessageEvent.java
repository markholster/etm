package com.jecstar.etm.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TelemetryMessageEvent extends TelemetryEvent {
	
	/**
	 * The content of the event.
	 */
	public String content;
	
	/**
	 * Data to be used for correlating event's that aren't correlated by the correlation id.
	 */
	public Map<String, String> correlationData = new HashMap<String, String>();
	
	/**
	 * The endpoint this event was send to, and received from.
	 */
	public String endpoint;

	/**
	 * The time after which the event expires.
	 */
	public Date expiryTime = new Date(0);
	
	/**
	 * Metadata of the event. Not used by the application, but can be filled by the end user. 
	 */
	public Map<String, String> metadata = new HashMap<String, String>();

	/**
	 * The name of the event.
	 */
	public String name;

	/**
	 * The name of the transaction this event belongs to. Transactions are groups of events that belong to a single unit of work.
	 */
	public String transactionName;

	/**
	 * The message type.
	 */
	public TelemetryMessageEventType type;

	/**
	 * The handlers that were reading the event.
	 */
	public List<EndpointHandler> readingEndpointHandlers = new ArrayList<EndpointHandler>();
	
	/**
	 * The handler that was writing the event.
	 */
	public EndpointHandler writingEndpointHandler = new EndpointHandler();
	
	/**
	 * Initialize the <code>TelemetryMessageEvent</code>.
	 * 
	 * @return The initialized <code>TelemetryMessageEvent</code>
	 */
	public TelemetryMessageEvent initialize() {
		this.id = null;
		this.content = null;
		this.correlationData.clear();
		this.correlationId = null;
		this.endpoint = null;
		this.expiryTime.setTime(0);
		this.metadata.clear();
		this.name = null;
		this.readingEndpointHandlers.clear();
		this.transactionName = null;
		this.type = null;
		this.writingEndpointHandler.initialize();
		return this;
	}
	
	public TelemetryMessageEvent initialize(TelemetryMessageEvent copy) {
	    initialize();
	    this.id = copy.id;
	    this.content = copy.content;
	    this.correlationData.putAll(copy.correlationData);
	    this.correlationId = copy.correlationId;
	    this.endpoint = copy.endpoint;
	    this.expiryTime.setTime(copy.expiryTime.getTime());
	    this.metadata.putAll(copy.metadata);
	    this.name = copy.name;
	    this.readingEndpointHandlers.addAll(copy.readingEndpointHandlers);
	    this.transactionName = copy.transactionName;
	    this.type = copy.type;
	    this.writingEndpointHandler.initialize(copy.writingEndpointHandler);
	    return this;
	}

	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TelemetryMessageEvent) {
			return ((TelemetryMessageEvent)obj).id.equals(this.id);
		}
	    return false;
	}
	
	@Override
	public int hashCode() {
	    return this.id.hashCode();
	}
	
}
