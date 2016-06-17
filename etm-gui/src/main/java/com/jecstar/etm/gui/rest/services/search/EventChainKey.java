package com.jecstar.etm.gui.rest.services.search;

public class EventChainKey {

	private final String eventId;
	private final String transactionId;

	EventChainKey(String eventId, String transactionId) {
		this.eventId = eventId;
		this.transactionId = transactionId;
	}
	
	public String getKey() {
		return this.eventId + "_" + this.transactionId;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof EventChainKey) {
			EventChainKey other = (EventChainKey) obj;
			return this.eventId.equals(other.eventId) && this.transactionId.equals(other.transactionId);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return (this.eventId + this.transactionId).hashCode();
	}

}
