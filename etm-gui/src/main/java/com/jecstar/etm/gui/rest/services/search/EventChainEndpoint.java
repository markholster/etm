package com.jecstar.etm.gui.rest.services.search;

import java.util.ArrayList;
import java.util.List;

public class EventChainEndpoint {
	
	private final String eventId;
	private final String endpointName;
	
	private EventChainKey writer;
	
	private List<EventChainKey> readers = new ArrayList<>();

	EventChainEndpoint(String eventId, String endpointName) {
		this.eventId = eventId;
		this.endpointName = endpointName;
	}

	public String getEndpointName() {
		return this.endpointName;
	}
	
	public EventChainKey getWriter() {
		return this.writer;
	}
	
	public List<EventChainKey> getReaders() {
		return this.readers;
	}
	
	public void setWriter(EventChainKey writer) {
		this.writer = writer;
	}

	public void addReader(EventChainKey reader) {
		this.readers.add(reader);
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
