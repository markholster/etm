package com.jecstar.etm.gui.rest.services.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventChain {
	
	private final Map<EventChainKey, EventChainConnectionData> readers = new HashMap<>();
	private final Map<EventChainKey, EventChainConnectionData> writers = new HashMap<>();
	private final List<EventChainEndpoint> endpoints = new ArrayList<>();
	private final List<EventChainTransactionEvents> transactions = new ArrayList<>();
	private final List<String> transactionIds = new ArrayList<>();
	private final List<String> eventIds = new ArrayList<>();
	
	public boolean containsTransaction(String transactionId) {
		return this.transactionIds.contains(transactionId);
	}
	
	public void addWriter(String eventId, String transactionId, String eventName, String eventType, String endpointName, String applicationName, long handlingTime) {
		EventChainKey key = new EventChainKey(eventId, transactionId);
		if (!this.writers.containsKey(key)) {
			this.writers.put(key, new EventChainConnectionData(eventId, transactionId, eventName, eventType, endpointName, applicationName, handlingTime));
		}
		if (eventId != null && endpointName != null) {
			EventChainEndpoint endpoint = new EventChainEndpoint(eventId, endpointName);
			int ix = this.endpoints.indexOf(endpoint);
			if (ix < 0) {
				this.endpoints.add(endpoint);
			} else {
				endpoint = this.endpoints.get(ix);
			}
			endpoint.setWriter(key);
		}
		if (transactionId != null) {
			EventChainTransactionEvents transactionEvent = new EventChainTransactionEvents(transactionId);
			int ix = this.transactions.indexOf(transactionEvent);
			if (ix < 0) {
				this.transactions.add(transactionEvent);
			} else {
				transactionEvent = this.transactions.get(ix);
			}
			transactionEvent.addWriter(key);
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
			this.readers.put(key, new EventChainConnectionData(eventId, transactionId, eventName, eventType, endpointName, applicationName, handlingTime));
		}
		if (eventId != null && endpointName != null) {
			EventChainEndpoint endpoint = new EventChainEndpoint(eventId, endpointName);
			int ix = this.endpoints.indexOf(endpoint);
			if (ix < 0) {
				this.endpoints.add(endpoint);
			} else {
				endpoint = this.endpoints.get(ix);
			}
			endpoint.addReader(key);
		}
		if (transactionId != null) {
			EventChainTransactionEvents transactionEvent = new EventChainTransactionEvents(transactionId);
			int ix = this.transactions.indexOf(transactionEvent);
			if (ix < 0) {
				this.transactions.add(transactionEvent);
			} else {
				transactionEvent = this.transactions.get(ix);
			}
			transactionEvent.setReader(key);
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
		return this.endpoints;
	}
	
	public List<EventChainTransactionEvents> getTransactions() {
		return this.transactions;
	}
	
	public Map<EventChainKey, EventChainConnectionData> getReaders() {
		return this.readers;
	}
	
	public Map<EventChainKey, EventChainConnectionData> getWriters() {
		return this.writers;
	}
	
}
