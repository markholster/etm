package com.jecstar.etm.gui.rest.services.search;

import java.util.ArrayList;
import java.util.List;

class EventChainEvent {
	
	private String eventId;
	
	private String endpointName;
	private EventChainItem reader;
	private List<EventChainItem> writers = new ArrayList<>();
	
	EventChainEvent(String eventId) {
		this.eventId = eventId;
	}

	public String getEventId() {
		return this.eventId;
	}
	
	public void setReader(EventChainItem item) {
		this.reader = item;
	}
	
	public EventChainItem getReader() {
		return this.reader;
	}
	
	public void addWriter(EventChainItem item) {
		if (!this.writers.contains(item)) {
			this.writers.add(item);
		}
	}
	
	public List<EventChainItem> getWriters() {
		return this.writers;
	}
	
	public void setEndpointName(String endpointName) {
		if (endpointName != null) {
			this.endpointName = endpointName;
		}
	}
	
	public String getEndpointName() {
		return this.endpointName;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof EventChainEvent) {
			EventChainEvent other = (EventChainEvent) obj;
			return this.eventId.equals(other.eventId);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.eventId.hashCode();
	}
	
}
