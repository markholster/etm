package com.jecstar.etm.gui.rest.services.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventChain {

	
	Map<String, EventChainTransaction> transactions = new HashMap<>();
	Map<String, EventChainEvent> events = new HashMap<>();
	
	public boolean containsTransaction(String transactionId) {
		return this.transactions.containsKey(transactionId);
	}
	
	public void addWriter(String eventId, String transactionId, String eventName, String eventType, String correlationId, String subType, String endpointName, String applicationName, long handlingTime, Long responseTime, Long expiry) {
		EventChainItem item = new EventChainItem(transactionId, eventId, handlingTime);
		item.setCorrelationId(correlationId)
			.setSubType(subType)
			.setName(eventName)
			.setApplicationName(applicationName)
			.setEventType(eventType)
			.setResponseTime(responseTime)
			.setExpiry(expiry);
		addItem(item, endpointName, true);
	}
	
	public void addReader(String eventId, String transactionId, String eventName, String eventType, String correlationId, String subType, String endpointName, String applicationName, long handlingTime, Long responseTime, Long expiry) {
		EventChainItem item = new EventChainItem(transactionId, eventId, handlingTime);
		item.setCorrelationId(correlationId)
			.setSubType(subType)
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
		if (item.getCorrelationId() != null) {
			event.setCorrelationId(item.getCorrelationId());
		}
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
		}
		for (EventChainEvent event : this.events.values()) {
			event.sort();
		}
		List<EventChainEvent> values = new ArrayList<>(this.events.values());
		values.sort(new Comparator<EventChainEvent>() {
			@Override
			public int compare(EventChainEvent o1, EventChainEvent o2) {
				return Long.compare(o1.getFirstEventChainItem().getHandlingTime(), o2.getFirstEventChainItem().getHandlingTime());
			}
		});
		if (values.isEmpty()) {
			return;
		}
		// If the root event is an http message and no writer present we have to make up one.
		EventChainEvent eventChainEvent = values.get(0);
		if (eventChainEvent.getWriter() == null) {
			EventChainItem reader = eventChainEvent.getReaders().get(0);
			if (reader.isRequest() && reader.isHttpEvent()) {
				eventChainEvent.setWriter(new EventChainItem(null, eventChainEvent.getEventId(), reader.getHandlingTime())
						.setApplicationName("Requestor")
						.setName(reader.getName())
						.setSubType(reader.getSubType())
						.setEventType(reader.getEventType())
						.setResponseTime(reader.getResponseTime())
						.setExpiry(reader.getExpiry())
						.setMissing(true));
				// Look for the response and hook it up to the requestor.
				EventChainEvent response = findResponse(eventChainEvent.getEventId());
				if (response != null && response.getReaders().isEmpty()) {
					response.addReader(new EventChainItem(null, response.getEventId(), response.getWriter().getHandlingTime())
							.setApplicationName("Requestor")
							.setName(response.getWriter().getName())
							.setSubType(response.getWriter().getSubType())
							.setEventType(response.getWriter().getEventType())
							.setMissing(true));
				}
			}
		}
	}

	public boolean isApplicationMissing(String application) {
		for (EventChainEvent event : this.events.values()) {
			if (event.getWriter() != null) {
				if (application.equals(event.getWriter().getApplicationName()) && !event.getWriter().isMissing()) {
					return false;
				}
			}
			for (EventChainItem item : event.getReaders()) {
				if (application.equals(item.getApplicationName()) && !item.isMissing()) {
					return false;
				}				
			}
		}
		return true;
	}
	
	private EventChainEvent findResponse(String id) {
		for (EventChainEvent event : this.events.values()) {
			if (event.isResponse() && id.equals(event.getCorrelationId())) {
				return event;
			}
		}
		return null;
	}
	
	private EventChainEvent findRequest(String correlationId) {
		for (EventChainEvent event : this.events.values()) {
			if (event.isRequest() && correlationId.equals(event.getEventId())) {
				return event;
			}
		}
		return null;
	}

	/**
	 * Calculate the percentage of time that was spent in the transition from
	 * the endpoint to given item.
	 */
	public Float calculateEdgePercentageFromEndpointToItem(EventChainEvent event, EventChainItem item) {
		if (event.getWriter() == null) {
			return null;
		}
		if (item.isMissing()) {
			return null;
		}
		long endpointTime = item.getHandlingTime() - event.getWriter().getHandlingTime();
		if (endpointTime <= 0) {
			return 0f;
		}
		// We found the time spent on the endpoint. Now find the root request to
		// calculate the percentage of time spend in the endpoint. We can't use
		// the the total event time, because this item can be in a async branch.
		EventChainEvent rootRequest = findRootRequest(event);
		if (rootRequest == null) {
			return null;
		}
		EventChainItem rootChainItem = rootRequest.getFirstEventChainItem();
		Long responseTime = rootChainItem.getResponseTime();
		if (responseTime == null && rootChainItem.getExpiry() != null) {
			responseTime = rootChainItem.getExpiry() - rootChainItem.getHandlingTime();
		}
		if (responseTime == null) {
			return null;
		}
		float percentage = (float)endpointTime / (float)responseTime;
		return percentage < 0.01f ? 0.01f : percentage;
	}

	public Float calculateEdgePercentageFromItemToItem(EventChainItem reader, EventChainItem writer) {
		if (reader.isMissing() || writer.isMissing()) {
			return null;
		}
		long handlingTime = writer.getHandlingTime() - reader.getHandlingTime();
		if (handlingTime <= 0) {
			return null;
		}
		EventChainEvent rootRequest = findRootRequest(this.events.get(reader.getEventId()));
		if (rootRequest == null) {
			return null;
		}
		Long responseTime = rootRequest.getFirstEventChainItem().getResponseTime();
		if (responseTime == null) {
			responseTime = rootRequest.getFirstEventChainItem().getExpiry();
		}
		if (responseTime == null) {
			return null;
		}
		float percentage = (float)handlingTime / (float)responseTime;
		return percentage < 0.01f ? 0.01f : percentage;
	}
	
	private EventChainEvent findRootRequest(EventChainEvent event) {
		if (event.isResponse()) {
			if (event.getCorrelationId() == null) {
				return null;
			}
			EventChainEvent request = findRequest(event.getCorrelationId());
			if (request == null) {
				return null;
			}
			return findRootRequest(request);
		}
		if (event.getWriter() != null && event.getWriter().getTransactionId() != null) {
			EventChainTransaction transaction = this.transactions.get(event.getWriter().getTransactionId());
			if (!transaction.getReaders().isEmpty()) {
				EventChainItem eventChainItem = transaction.getReaders().get(0);
				EventChainEvent parent = this.events.get(eventChainItem.getEventId());
				if (!parent.isAsync()) {
					return findRootRequest(parent);
				}
			}
		}
		if (event.isRequest()) {
			return event;
		}
		return null;
	}


}
