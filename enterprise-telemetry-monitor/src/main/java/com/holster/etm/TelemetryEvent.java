package com.holster.etm;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.datastax.driver.core.utils.UUIDs;


public class TelemetryEvent {
	
	public static final String JMS_PROPERTY_KEY_SOURCE_ID = "JMS_ETM_SourceID";
	public static final String JMS_PROPERTY_KEY_SOURCE_CORRELATION_ID = "JMS_ETM_SourceCorrelationID";
	public static final String JMS_PROPERTY_KEY_ENDPOINT = "JMS_ETM_Endpoint";
	public static final String JMS_PROPERTY_KEY_APPLICATION = "JMS_ETM_Application";
	public static final String JMS_PROPERTY_KEY_EVENT_NAME = "JMS_ETM_EventName";
	public static final String JMS_PROPERTY_KEY_TRANSACTION_ID = "JMS_ETM_TransactionID";
	public static final String JMS_PROPERTY_KEY_TRANSACTION_NAME = "JMS_ETM_TransactionName";
	public static final String JMS_PROPERTY_KEY_MESSAGE_TYPE = "JMS_ETM_MessageType";

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
	 * The type of event.
	 */
	public TelemetryEventType eventType;
	
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
	 * The time difference between the correlating event 's event time, and this event's event time.
	 */
	public long correlationTimeDifference;
	
	/**
	 * Data to be used for correlating event's that aren't correlated by the correlation id.
	 */
	public Map<String, String> correlationData = new HashMap<String, String>();
	
	// Processing state.
	public boolean ignore;
	
	
	public TelemetryEvent initialize() {
		this.id = UUIDs.timeBased();
		this.content = null;
		this.correlationTimeDifference = 0;
		this.correlationId = null;
		this.transactionId = null;
		this.transactionName = null;
		this.endpoint = null;
		this.application = null;
		this.eventName = null;
		this.eventTime.setTime(0);
		this.eventType = null;
		this.sourceId = null;
		this.sourceCorrelationId = null;
		this.ignore = false;
		this.correlationData.clear();
		return this;
	}


	public TelemetryEvent initialize(TelemetryEvent copy) {
	    initialize();
	    this.content = copy.content;
	    this.correlationTimeDifference = copy.correlationTimeDifference;
	    this.correlationId = copy.correlationId;
	    this.transactionId = copy.transactionId;
	    this.transactionName = copy.transactionName;
	    this.endpoint = copy.endpoint;
	    this.application = copy.application;
	    this.eventName = copy.eventName;
	    this.eventTime.setTime(copy.eventTime.getTime());
	    this.eventType = copy.eventType;
	    this.sourceId = copy.sourceId;
	    this.sourceCorrelationId = copy.sourceCorrelationId;
	    this.correlationData.putAll(copy.correlationData);
	    return this;
    }
}
