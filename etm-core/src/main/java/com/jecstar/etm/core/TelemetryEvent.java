package com.jecstar.etm.core;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TelemetryEvent implements Cloneable {
	
	/**
	 * The unique ID of the event.
	 */
	public String id;
	
	/**
	 * The ID of the event this event is correlated to. This is mainly used match a response to a certain request.
	 */
	public String correlationId;
	
	/**
	 * The name of the event.
	 */
	public String name;
	
	/**
	 * The type of event.
	 */
	public TelemetryEventType type;

	/**
	 * The direction of the event.
	 */
	public TelemetryEventDirection direction;
	
	/**
	 * The ID of the transaction this event belongs to. Transactions are groups of events that belong to a single unit of work.
	 */
	public String transactionId;
	
	/**
	 * The name of the transaction this event belongs to. Transactions are groups of events that belong to a single unit of work.
	 */
	public String transactionName;
	
	/**
	 * The content of the event.
	 */
	public String content;
	
	/**
	 * The endpoint this event was send to, or received from.
	 */
	public String endpoint;
	
	/**
	 * The application that send or received the event,
	 */
	public String application;
	
	/**
	 * The time the event was originally created.
	 */
	public Date creationTime = new Date(0);

	/**
	 * The time after which the event expires.
	 */
	public Date expiryTime = new Date(0);
	
	/**
	 * Metadata of the event. Not used by the application, but can be filled by the end user. 
	 */
	public Map<String, String> metadata = new HashMap<String, String>();
	
	/**
	 * Data to be used for correlating event's that aren't correlated by the correlation id.
	 */
	public Map<String, String> correlationData = new HashMap<String, String>();
	
	// Processing state.
	public EventCommand eventCommand;
	public boolean ignore;
	
	public TelemetryEvent initialize() {
		this.id = null;
		this.application = null;
		this.content = null;
		this.correlationData.clear();
		this.correlationId = null;
		this.creationTime.setTime(0);
		this.direction = null;
		this.endpoint = null;
		this.expiryTime.setTime(0);
		this.metadata.clear();
		this.name = null;
		this.transactionId = null;
		this.transactionName = null;
		this.type = null;
		this.ignore = false;
		this.eventCommand = EventCommand.PROCESS;
		return this;
	}


	public TelemetryEvent initialize(TelemetryEvent copy) {
	    initialize();
	    this.id = copy.id;
		this.application = copy.application;
		this.content = copy.content;
		this.correlationData.putAll(copy.correlationData);
		this.correlationId = copy.correlationId;
		this.creationTime.setTime(copy.creationTime.getTime());
		this.direction = copy.direction;
		this.endpoint = copy.endpoint;
		this.expiryTime.setTime(copy.expiryTime.getTime());
		this.metadata.putAll(copy.metadata);
		this.name = copy.name;
		this.transactionId = copy.transactionId;
		this.transactionName = copy.transactionName;
		this.type = copy.type;
		this.eventCommand = copy.eventCommand;
	    return this;
    }
	
	@Override
	public TelemetryEvent clone() {
		TelemetryEvent clone = new TelemetryEvent();
		clone.initialize(this);
		clone.ignore = this.ignore;
		clone.id = this.id;
	    return clone;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TelemetryEvent) {
			return ((TelemetryEvent)obj).id.equals(this.id);
		}
	    return false;
	}
	
	@Override
	public int hashCode() {
	    return this.id.hashCode();
	}
}
