package com.jecstar.etm.gui.rest.services.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventChain {

	
	Map<String, EventChainTransaction> transactions = new HashMap<>();
	Map<String, EventChainEvent> events = new HashMap<>();
	
	public boolean containsTransaction(String transactionId) {
		return this.transactions.containsKey(transactionId);
	}
	
	public void addWriter(String eventId, String transactionId, String eventName, String eventType, String endpointName, String applicationName, long handlingTime) {
		EventChainItem item = new EventChainItem(transactionId, eventId, handlingTime);
		item.setName(eventName).setApplicationName(applicationName).setEventType(eventType);
		addItem(item, endpointName, true);
	}
	
	public void addReader(String eventId, String transactionId, String eventName, String eventType, String endpointName, String applicationName, long handlingTime) {
		EventChainItem item = new EventChainItem(transactionId, eventId, handlingTime);
		item.setName(eventName).setApplicationName(applicationName).setEventType(eventType);
		addItem(item, endpointName, false);		
	}
	
	private void addItem(EventChainItem item, String endpointName, boolean writer) {
		if (item.getTransactionId() != null) {
			EventChainTransaction transaction = this.transactions.get(item.getTransactionId());
			if (transaction == null) {
				transaction = new EventChainTransaction(item.getTransactionId());
				this.transactions.put(item.getTransactionId(), transaction);
			}
			if (writer) {
				transaction.addWriter(item);
			} else {
				transaction.addReader(item);
			}
		}
		EventChainEvent event = this.events.get(item.getEventId());
		if (event == null) {
			event = new EventChainEvent(item.getEventId());
			this.events.put(item.getEventId(), event);
		}
		event.setEndpointName(endpointName);
		if (writer) {
			event.addWriter(item);
		} else {
			event.setReader(item);
		}
	}
	
	public List<String> getApplications() {
		List<String> result = new ArrayList<String>();
		for (EventChainEvent event : this.events.values()) {
			if (event.getReader() != null 
					&& event.getReader().getApplicationName() != null
					&& !result.contains(event.getReader().getApplicationName())) {
				result.add(event.getReader().getApplicationName());
			}
			for (EventChainItem item : event.getWriters()) {
				if (item.getApplicationName() != null && !result.contains(item.getApplicationName())) {
					result.add(item.getApplicationName());
				}
			}
		}
		return result;
	}
}
