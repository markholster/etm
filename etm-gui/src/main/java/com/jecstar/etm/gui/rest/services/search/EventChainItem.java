package com.jecstar.etm.gui.rest.services.search;

class EventChainItem {

	private final String eventId;
	private final String transactionId;
	private long handlingTime;
	private String correlationId;
	private String name;
	private String applicationName;
	private String eventType;
	private Long responseTime;
	private Long expiry;
	private boolean request;

	EventChainItem(String transactionId, String eventId, long handlingTime) {
		this.transactionId = transactionId;
		this.eventId = eventId;
		this.handlingTime = handlingTime;
	}
	
	public String getKey() {
		return this.transactionId + "_" + this.eventId;
	}

	public String getTransactionId() {
		return this.transactionId;
	}
	
	public String getEventId() {
		return this.eventId;
	}
	
	public long getHandlingTime() {
		return this.handlingTime;
	}
	
	public EventChainItem setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
		return this;
	}
	
	public String getCorrelationId() {
		return this.correlationId;
	}
	
	public EventChainItem setRequest(boolean request) {
		this.request = request;
		return this;
	}

	public boolean isRequest() {
		return request;
	}
	
	public EventChainItem setName(String name) {
		this.name = name;
		return this;
	}
	
	public String getName() {
		return this.name;
	}
	
	public EventChainItem setApplicationName(String applicationName) {
		this.applicationName = applicationName;
		return this;
	}
	
	public String getApplicationName() {
		return this.applicationName;
	}

	public EventChainItem setEventType(String eventType) {
		this.eventType = eventType;
		return this;
	}
	
	public String getEventType() {
		return this.eventType;
	}
	
	public EventChainItem setResponseTime(Long responseTime) {
		this.responseTime = responseTime;
		return this;
	}
	
	public Long getResponseTime() {
		return this.responseTime;
	}
	
	public EventChainItem setExpiry(Long expiry) {
		this.expiry = expiry;
		return this;
	}
	
	public Long getExpiry() {
		return this.expiry;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof EventChainItem) {
			EventChainItem other = (EventChainItem) obj;
			return getKey().equals(other.getKey());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return getKey().hashCode();
	}
}
