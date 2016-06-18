package com.jecstar.etm.gui.rest.services.search;

class EventChainItem {

	private final String eventId;
	private final String transactionId;
	private long handlingTime;
	private String name;
	private String applicationName;
	private String eventType;

	public EventChainItem(String transactionId, String eventId, long handlingTime) {
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
