package com.jecstar.etm.gui.rest.services.search;

public class EventChainConnectionData {

	private final String eventId;
	private final String transactionId;
	private final String eventName;
	private final String eventType;
	private final String endpointName;
	private final String applicationName;
	private final long handlingTime;

	EventChainConnectionData(String eventId, String transactionId, String eventName, String eventType, String endpointName, String applicationName, long handlingTime) {
		this.eventId = eventId;
		this.transactionId = transactionId;
		this.eventName = eventName;
		this.eventType = eventType;
		this.endpointName = endpointName;
		this.applicationName = applicationName;
		this.handlingTime = handlingTime;
	}
	
	public String getEventId() {
		return this.eventId;
	}
	
	public String getTransactionId() {
		return this.transactionId;
	}
	
	public String getEventName() {
		return this.eventName;
	}
	
	public String getEventType() {
		return this.eventType;
	}
	
	public String getEndpointName() {
		return this.endpointName;
	}
	
	public String getApplicationName() {
		return this.applicationName;
	}
	
	public long getHandlingTime() {
		return this.handlingTime;
	}
	
}
