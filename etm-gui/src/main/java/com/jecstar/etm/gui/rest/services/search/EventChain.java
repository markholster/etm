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
	
	public void addWriter(String eventId, String transactionId, String eventName, String eventType, String correlationId, boolean request, String endpointName, String applicationName, long handlingTime, Long responseTime, Long expiry) {
		EventChainItem item = new EventChainItem(transactionId, eventId, handlingTime);
		item.setCorrelationId(correlationId)
			.setRequest(request)
			.setName(eventName)
			.setApplicationName(applicationName)
			.setEventType(eventType)
			.setResponseTime(responseTime)
			.setExpiry(expiry);
		addItem(item, endpointName, true);
	}
	
	public void addReader(String eventId, String transactionId, String eventName, String eventType, String correlationId, boolean request, String endpointName, String applicationName, long handlingTime, Long responseTime, Long expiry) {
		EventChainItem item = new EventChainItem(transactionId, eventId, handlingTime);
		item.setCorrelationId(correlationId)
			.setRequest(request)
			.setName(eventName)
			.setApplicationName(applicationName)
			.setEventType(eventType)
			.setResponseTime(responseTime)
			.setExpiry(expiry);
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
			event.setWriter(item);
		} else {
			event.addReader(item);
		}
	}
	
	public List<String> getApplications() {
		List<String> result = new ArrayList<String>();
		for (EventChainEvent event : this.events.values()) {
			if (event.getWriter() != null 
					&& event.getWriter().getApplicationName() != null
					&& !result.contains(event.getWriter().getApplicationName())) {
				result.add(event.getWriter().getApplicationName());
			}
			for (EventChainItem item : event.getReaders()) {
				if (item.getApplicationName() != null && !result.contains(item.getApplicationName())) {
					result.add(item.getApplicationName());
				}
			}
		}
		return result;
	}

	public void done() {
		for (EventChainTransaction transaction : this.transactions.values()) {
			transaction.sort();
			transaction.calculateAbsoluteResponseTimes();
		}
		for (EventChainEvent event : this.events.values()) {
			event.sort();
			if (event.isRequest()) {
				EventChainItem rootRequest = getRootRequest(event);
				if (rootRequest != null) {
					Long transactionTime = rootRequest.getResponseTime();
					if (transactionTime == null && rootRequest.getExpiry() != null) {
						transactionTime = rootRequest.getExpiry() - rootRequest.getHandlingTime();
					}
					if (transactionTime != null) {
						event.calculateResponseTimePercentages(transactionTime);
					}
				}
			}
		}
	}

	private EventChainItem getRootRequest(EventChainEvent event) {
		if (event.isRequest()) {
			if (event.getWriter() != null) {
				EventChainTransaction eventChainTransaction = this.transactions.get(event.getWriter().getTransactionId());
				if (eventChainTransaction.getReaders().isEmpty()) {
					return event.getWriter();
				}
				EventChainItem reader = eventChainTransaction.getReaders().get(0);
				if (reader.getEventId().equals(event.getEventId())) {
					return reader;
				}
				if (reader.isRequest()) {
					return getRootRequest(this.events.get(reader.getEventId()));
				}
			}
			return event.getReaders().get(0);
		} else {
			return null;
			// TODO dit is een datagram of een response. Bijbehorende spullen zoeken.
		}
	}
}
