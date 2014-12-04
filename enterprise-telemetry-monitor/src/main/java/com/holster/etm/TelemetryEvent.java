package com.holster.etm;

import java.util.Date;
import java.util.UUID;


public class TelemetryEvent {

	/**
	 * The unique ID of the event.
	 */
	public UUID id;
	
	/**
	 * The ID of the event this event is correlated to. This is mainly used match a response to a certain request.
	 */
	public UUID correlationId;
	
	/**
	 * The name of the event.
	 */
	public String eventName;
	
	/**
	 * The ID of the transaction this event belongs to. Transactions are groups of events that belong to a single unit of work.
	 */
	public UUID transactionId;
	
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
	 * The time the event was fired.
	 */
	public Date eventTime = new Date(0);
	
	/**
	 * The ID of the source of this event, for example a JMSMessageID.
	 */
	public String sourceId;
	
	/**
	 * The correlating source ID, for example a JMSCorrelationID.
	 */
	public String sourceCorrelationId;
	
	/**
	 * The source object of this event, for example a <code>javax.jms.Message</code> instance.;
	 */
	public Object source;
	
	// Processing state.
	public boolean ignore;
	
	
	public void initialize() {
		this.id = UUID.randomUUID();
		this.eventName = null;
		this.correlationId = null;
		this.transactionId = null;
		this.transactionName = null;
		this.content = null;
		this.endpoint = null;
		this.application = null;
		this.eventTime.setTime(0);
		this.sourceId = null;
		this.sourceCorrelationId = null;
		this.source = null;
		this.ignore = false;
	}


	public void initialize(TelemetryEvent copy) {
	    initialize();
	    this.eventName = copy.eventName;
	    this.correlationId = copy.correlationId;
	    this.transactionId = copy.transactionId;
	    this.transactionName = copy.transactionName;
	    this.content = copy.content;
	    this.endpoint = copy.endpoint;
	    this.application = copy.application;
	    this.eventTime.setTime(copy.eventTime.getTime());
	    this.sourceId = copy.sourceId;
	    this.sourceCorrelationId = copy.sourceCorrelationId;
	    this.source = copy.source;
    }
}
