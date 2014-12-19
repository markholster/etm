package com.holster.etm;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.datastax.driver.core.utils.UUIDs;


public class TelemetryEvent {
	
	public static final String JMS_PROPERTY_KEY_EVENT_APPLICATION = "JMS_ETM_Application";
	public static final String JMS_PROPERTY_KEY_EVENT_DIRECTION = "JMS_ETM_Direction";
	public static final String JMS_PROPERTY_KEY_EVENT_ENDPOINT = "JMS_ETM_Endpoint";
	public static final String JMS_PROPERTY_KEY_EVENT_NAME = "JMS_ETM_Name";
	public static final String JMS_PROPERTY_KEY_EVENT_SOURCE_CORRELATION_ID = "JMS_ETM_SourceCorrelationID";
	public static final String JMS_PROPERTY_KEY_EVENT_SOURCE_ID = "JMS_ETM_SourceID";
	public static final String JMS_PROPERTY_KEY_EVENT_TRANSACTION_NAME = "JMS_ETM_TransactionName";
	public static final String JMS_PROPERTY_KEY_EVENT_TYPE = "JMS_ETM_Type";
	
	public static final String CORRELATION_KEY_SOURCE_ID = "ETM__Source_ID";

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
	 * The time the event was originally created.
	 */
	public Date creationTime = new Date(0);

	/**
	 * The time after which the event expires.
	 */
	public Date expiryTime = new Date(0);
	
	/**
	 * The ID of the source of this event, for example a JMSMessageID.
	 */
	public String sourceId;
	
	/**
	 * The correlating source ID, for example a JMSCorrelationID.
	 */
	public String sourceCorrelationId;
	
	/**
	 * The time the response took.
	 */
	public long responseTime;
	
	/**
	 * Data to be used for correlating event's that aren't correlated by the correlation id.
	 */
	public Map<String, String> correlationData = new HashMap<String, String>();
	
	// Processing state.
	public boolean ignore;
	
	
	public TelemetryEvent initialize() {
		this.id = UUIDs.timeBased();
		this.application = null;
		this.content = null;
		this.correlationData.clear();
		this.correlationId = null;
		this.creationTime.setTime(0);
		this.direction = null;
		this.endpoint = null;
		this.expiryTime.setTime(0);
		this.name = null;
		this.sourceCorrelationId = null;
		this.sourceId = null;
		this.transactionId = null;
		this.transactionName = null;
		this.type = null;
		this.responseTime = 0;
		this.ignore = false;
		return this;
	}


	public TelemetryEvent initialize(TelemetryEvent copy) {
	    initialize();
		this.application = copy.application;
		this.content = copy.content;
		this.correlationData.putAll(copy.correlationData);
		this.correlationId = copy.correlationId;
		this.creationTime.setTime(copy.creationTime.getTime());
		this.direction = copy.direction;
		this.endpoint = copy.endpoint;
		this.expiryTime.setTime(copy.expiryTime.getTime());
		this.name = copy.name;
		this.sourceCorrelationId = copy.sourceCorrelationId;
		this.sourceId = copy.sourceId;
		this.transactionId = copy.transactionId;
		this.transactionName = copy.transactionName;
		this.type = copy.type;
		this.responseTime = copy.responseTime;
	    return this;
    }
}
