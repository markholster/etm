package com.jecstar.etm.gui.rest.services.search;

public class EventChainEndpoint {
	
	private final String eventId;
	private final String endpointName;

	EventChainEndpoint(String eventId, String endpointName) {
		this.eventId = eventId;
		this.endpointName = endpointName;
	}

	
	public String getEndpointName() {
		return this.endpointName;
	}
	
	public String getKey() {
		return this.eventId + "_" + this.endpointName;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof EventChainEndpoint) {
			EventChainEndpoint other = (EventChainEndpoint) obj;
			return this.eventId.equals(other.eventId) && this.endpointName.equals(other.endpointName);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return (this.eventId + this.endpointName).hashCode();
	}
}
