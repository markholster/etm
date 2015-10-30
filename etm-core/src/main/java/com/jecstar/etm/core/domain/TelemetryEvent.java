package com.jecstar.etm.core.domain;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TelemetryEvent {
	
	public static String PACKAGING_DB_DELETE = "DELETE";
	public static String PACKAGING_DB_INSERT = "INSERT";
	public static String PACKAGING_DB_SELECT = "SELECT";
	public static String PACKAGING_DB_UPDATE = "UPDATE";
	public static String PACKAGING_DB_RESULT = "Response (DB)";
	public static String PACKAGING_HTTP_CONNECT = "CONNECT";
	public static String PACKAGING_HTTP_DELETE = "DELETE";
	public static String PACKAGING_HTTP_GET = "GET";
	public static String PACKAGING_HTTP_HEAD = "HEAD";
	public static String PACKAGING_HTTP_OPTIONS = "OPTIONS";
	public static String PACKAGING_HTTP_POST = " POST";
	public static String PACKAGING_HTTP_PUT = "PUT";
	public static String PACKAGING_HTTP_TRACE = "TRACE";
	public static String PACKAGING_HTTP_RESPONSE = "Response (HTTP)";
	public static String PACKAGING_LOG = "Log";
	public static String PACKAGING_MQ_FIRE_AND_FORGET = "Fire And forget";
	public static String PACKAGING_MQ_REQUEST = "Request";
	public static String PACKAGING_MQ_RESPONSE = "Response (MQ)";
	public static String PACKAGING_SMTP_EMAIL = "Email";

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
	 * The moment this event expires, in case of a request.
	 */
	public ZonedDateTime expiry;
	
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
	 * The packaging of the payload. E.g. an Email of an MQ Request.
	 */
	public String packaging;
	
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
	 * The transport type used to send the {@link #payload} to from the sending application to the receiving application(s).
	 */
	public Transport transport;

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
		this.endpoint = null;
		this.expiry = null;
		this.extractedData.clear();
		this.metadata.clear();
		this.packaging = null;
		this.payload = null;
		this.payloadFormat = null;
		this.readingEndpointHandlers.clear();
		this.transactionId = null;
		this.transport = null;
		this.writingEndpointHandler.initialize();
		return this;
	}
	
	public TelemetryEvent initialize(TelemetryEvent copy) {
		this.initialize();
		if (copy == null) {
			return this;
		}
		this.id = copy.id;
		this.correlationId = copy.correlationId;
		this.correlationData.putAll(copy.correlationData);
		this.endpoint = copy.endpoint;
		this.expiry = copy.expiry;
		this.extractedData.putAll(copy.extractedData);
		this.metadata.putAll(copy.metadata);
		this.packaging = copy.packaging;
		this.payload = copy.payload;
		this.payloadFormat = copy.payloadFormat;
		this.readingEndpointHandlers.addAll(copy.readingEndpointHandlers);
		this.transactionId = copy.transactionId;
		this.transport = copy.transport;
		this.writingEndpointHandler.initialize(copy.writingEndpointHandler);
		return this;
	}
	
	public ZonedDateTime getEventTime() {
		if (this.writingEndpointHandler.handlingTime != null) {
			return this.writingEndpointHandler.handlingTime;
		}
		return this.readingEndpointHandlers.stream().sorted((h1, h2) -> h1.handlingTime.compareTo(h2.handlingTime)).findFirst().get().handlingTime;
	}
	
	public boolean isResponse() {
		if (this.packaging == null) {
			return false;
		}
		return this.packaging.startsWith("Response (");
	}
	
	public boolean isRequest() {
		if (this.packaging == null) {
			return false;
		}
		return !isResponse() && !PACKAGING_MQ_FIRE_AND_FORGET.equals(this.packaging);
	}
}
