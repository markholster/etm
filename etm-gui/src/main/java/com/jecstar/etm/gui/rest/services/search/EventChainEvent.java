package com.jecstar.etm.gui.rest.services.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class EventChainEvent {
	
	private String eventId;
	private String correlationId;
	
	private List<EventChainEndpoint> endpoints = new ArrayList<>();
	
	private Comparator<EventChainEndpoint> handlingTimeComparator = new Comparator<EventChainEndpoint>(){
		@Override
		public int compare(EventChainEndpoint o1, EventChainEndpoint o2) {
			return Long.compare(o1.getFirstEventChainItem().getHandlingTime(), o2.getFirstEventChainItem().getHandlingTime());
		}};

	EventChainEvent(String eventId) {
		this.eventId = eventId;
	}

	public String getEventId() {
		return this.eventId;
	}
	

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}
	
	public String getCorrelationId() {
		return this.correlationId;
	}
	
	
	public void addEndpoint(EventChainEndpoint endpoint) {
		if (!this.endpoints.contains(endpoint)) {
			this.endpoints.add(endpoint);
		}
	}
	
	public List<EventChainEndpoint> getEndpoints() {
		return this.endpoints;
	}
	
	public EventChainEndpoint getEndpoint(String name) {
		 EventChainEndpoint endpoint = new EventChainEndpoint(name, this.eventId);
		 int ix = this.endpoints.indexOf(endpoint); 
		 if (ix >= 0) {
			 return this.endpoints.get(ix);
		 }
		 return null;
	}
		
	public boolean isRequest() {
		return getFirstEventChainItem().isRequest();
	}
	
	public boolean isResponse() {
		return getFirstEventChainItem().isResponse();
	}
	
	public boolean isAsync() {
		return getFirstEventChainItem().isAsync();
	}
	
	public void sort() {
		for (EventChainEndpoint endpoint : this.endpoints) {
			endpoint.sort();
		}
		this.endpoints.sort(this.handlingTimeComparator);
	}
	
	/**
	 * Gives the first item that occurred in this event. This is the writer, or
	 * the first reader if no writer is present. Make sure the {@link #sort()}
	 * method is called before calling this method if you want to retrieve the
	 * first element in time.
	 * 
	 * @return The first item.
	 */
	public EventChainItem getFirstEventChainItem() {
		return this.endpoints.get(0).getFirstEventChainItem();
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
