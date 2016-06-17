package com.jecstar.etm.gui.rest.services.search;

public class EventChainConnectionData {

	private final String eventName;
	private final String eventType;
	private final String endpointName;
	private final String applicationName;
	private final long handlingTime;

	EventChainConnectionData(String eventName, String eventType, String endpointName, String applicationName, long handlingTime) {
		this.eventName = eventName;
		this.eventType = eventType;
		this.endpointName = endpointName;
		this.applicationName = applicationName;
		this.handlingTime = handlingTime;
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
