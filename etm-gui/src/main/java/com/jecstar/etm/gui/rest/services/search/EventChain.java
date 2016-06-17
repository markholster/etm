package com.jecstar.etm.gui.rest.services.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventChain {
	
	private final Map<EventChainKey, EventChainConnectionData> readers = new HashMap<>();
	private final Map<EventChainKey, EventChainConnectionData> writers = new HashMap<>();
	private final List<EventChainEndpoint> eventChainEndpoints = new ArrayList<>();
	private final List<String> transactionIds = new ArrayList<>();
	private final List<String> eventIds = new ArrayList<>();
	
	public boolean containsTransaction(String transactionId) {
		return this.transactionIds.contains(transactionId);
	}
	
	public void addWriter(String eventId, String transactionId, String eventName, String eventType, String endpointName, String applicationName, long handlingTime) {
		EventChainKey key = new EventChainKey(eventId, transactionId);
		if (!this.writers.containsKey(key)) {
			this.writers.put(key, new EventChainConnectionData(eventName, eventType, endpointName, applicationName, handlingTime));
		}
		if (eventId != null && endpointName != null) {
			EventChainEndpoint endpoint = new EventChainEndpoint(eventId, endpointName);
			if (!this.eventChainEndpoints.contains(endpoint)) {
				this.eventChainEndpoints.add(endpoint);
			}
		}
		if (!this.transactionIds.contains(transactionId)) {
			this.transactionIds.add(transactionId);
		}
		if (!this.eventIds.contains(eventId)) {
			this.eventIds.add(eventId);
		}
	}
	
	public void addReader(String eventId, String transactionId, String eventName, String eventType, String endpointName, String applicationName, long handlingTime) {
		EventChainKey key = new EventChainKey(eventId, transactionId);
		if (!this.readers.containsKey(key)) {
			this.readers.put(key, new EventChainConnectionData(eventName, eventType, endpointName, applicationName, handlingTime));
		}
		if (eventId != null && endpointName != null) {
			EventChainEndpoint endpoint = new EventChainEndpoint(eventId, endpointName);
			if (!this.eventChainEndpoints.contains(endpoint)) {
				this.eventChainEndpoints.add(endpoint);
			}
		}
		if (!this.transactionIds.contains(transactionId)) {
			this.transactionIds.add(transactionId);
		}
		if (!this.eventIds.contains(eventId)) {
			this.eventIds.add(eventId);
		}
	}
	
	public List<String> getApplications() {
		List<String> result = new ArrayList<String>();
		for (EventChainConnectionData connectionData : this.readers.values()) {
			if (connectionData.getApplicationName() != null && !result.contains(connectionData.getApplicationName())) {
				result.add(connectionData.getApplicationName());
			}
		}
		for (EventChainConnectionData connectionData : this.writers.values()) {
			if (connectionData.getApplicationName() != null && !result.contains(connectionData.getApplicationName())) {
				result.add(connectionData.getApplicationName());
			}
		}		
		return result;
	}
	
	public List<EventChainEndpoint> getEventChainEndpoints() {
		return this.eventChainEndpoints;
	}
	
	public Map<EventChainKey, EventChainConnectionData> getReaders() {
		return this.readers;
	}
	
	public Map<EventChainKey, EventChainConnectionData> getWriters() {
		return this.writers;
	}
	
}
