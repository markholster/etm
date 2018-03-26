package com.jecstar.etm.gui.rest.services.search.eventchain;

import java.util.*;

public class EventChain {


    private final Map<String, EventChainTransaction> transactions = new HashMap<>();
    private final Map<String, EventChainEvent> events = new HashMap<>();

    public boolean containsTransaction(String transactionId) {
        return this.transactions.containsKey(transactionId);
    }

    public void addWriter(String eventId,
                          String transactionId,
                          String eventName,
                          String eventType,
                          String correlationId,
                          String subType,
                          String endpointName,
                          String applicationName,
                          String applicationInstance,
                          long handlingTime,
                          Long responseTime,
                          Long expiry) {
        EventChainItem item = new EventChainItem(transactionId, eventId, handlingTime);
        item.setCorrelationId(correlationId)
                .setSubType(subType)
                .setName(eventName)
                .setApplication(applicationName, applicationInstance)
                .setEventType(eventType)
                .setResponseTime(responseTime)
                .setExpiry(expiry);
        addItem(item, endpointName, true);
    }

    public void addReader(String eventId,
                          String transactionId,
                          String eventName,
                          String eventType,
                          String correlationId,
                          String subType,
                          String endpointName,
                          String applicationName,
                          String applicationInstance,
                          long handlingTime,
                          Long responseTime,
                          Long expiry) {
        EventChainItem item = new EventChainItem(transactionId, eventId, handlingTime);
        item.setCorrelationId(correlationId)
                .setSubType(subType)
                .setName(eventName)
                .setApplication(applicationName, applicationInstance)
                .setEventType(eventType)
                .setResponseTime(responseTime)
                .setExpiry(expiry);
        addItem(item, endpointName, false);
    }

    private void addItem(EventChainItem item, String endpointName, boolean writer) {
        if (item.getTransactionId() != null) {
            EventChainTransaction transaction = this.transactions.computeIfAbsent(item.getTransactionId(), k -> new EventChainTransaction(item.getTransactionId()));
            if (writer) {
                transaction.addWriter(item);
            } else {
                transaction.addReader(item);
            }
        }
        EventChainEvent event = this.events.computeIfAbsent(item.getEventId(), k -> new EventChainEvent(item.getEventId(), item.getEventType()));
        EventChainEndpoint endpoint = event.getEndpoint(endpointName);
        if (endpoint == null) {
            endpoint = new EventChainEndpoint(endpointName, item.getEventId());
            event.addEndpoint(endpoint);
        }
        if (item.getCorrelationId() != null) {
            event.setCorrelationId(item.getCorrelationId());
        }
        if (writer) {
            endpoint.setWriter(item);
        } else {
            endpoint.addReader(item);
        }
    }

    public List<EventChainApplication> getApplications() {
        List<EventChainApplication> result = new ArrayList<>();
        for (EventChainEvent event : this.events.values()) {
            for (EventChainEndpoint endpoint : event.getEndpoints()) {
                if (endpoint.getWriter() != null
                        && endpoint.getWriter().getApplication() != null
                        && !result.contains(endpoint.getWriter().getApplication())) {
                    result.add(endpoint.getWriter().getApplication());
                }
                for (EventChainItem item : endpoint.getReaders()) {
                    if (item.getApplication() != null && !result.contains(item.getApplication())) {
                        result.add(item.getApplication());
                    }
                }
            }
        }
        return result;
    }

    public Map<String, EventChainEvent> getEvents() {
        return this.events;
    }

    public Map<String, EventChainTransaction> getTransactions() {
        return this.transactions;
    }

    public void done() {
        for (EventChainTransaction transaction : this.transactions.values()) {
            transaction.sort();
        }
        for (EventChainEvent event : this.events.values()) {
            event.sort();
        }
        List<EventChainEvent> values = new ArrayList<>(this.events.values());
        values.sort(Comparator.comparingLong(o -> o.getFirstEventChainItem().getHandlingTime()));
        if (values.isEmpty()) {
            return;
        }
        // If the root event is an http message and no writer present we have to make up one.
//		EventChainEvent eventChainEvent = values.get(0);
//		EventChainEndpoint endpoint = eventChainEvent.getEndpoints().get(0);
//		if (endpoint.getWriter() == null) {
//			EventChainItem reader = endpoint.getReaders().get(0);
//			if (reader.isRequest() && reader.isHttpEvent()) {
//				endpoint.setWriter(new EventChainItem(null, eventChainEvent.getEventId(), reader.getHandlingTime())
//						.setApplicationName("Requestor")
//						.setName(reader.getName())
//						.setSubType(reader.getSubType())
//						.setEventType(reader.getEventType())
//						.setResponseTime(reader.getResponseTime())
//						.setExpiry(reader.getExpiry())
//						.setMissing(true));
//				// Look for the response and hook it up to the requestor.
//				EventChainEvent response = findResponse(eventChainEvent.getEventId());
//				if (response != null && response.getReaders().isEmpty()) {
//					response.addReader(new EventChainItem(null, response.getEventId(), response.getWriter().getHandlingTime())
//							.setApplicationName("Requestor")
//							.setName(response.getWriter().getName())
//							.setSubType(response.getWriter().getSubType())
//							.setEventType(response.getWriter().getEventType())
//							.setMissing(true));
//				}
//			}
//		}
    }

    public boolean isApplicationMissing(EventChainApplication application) {
        for (EventChainEvent event : this.events.values()) {
            for (EventChainEndpoint endpoint : event.getEndpoints()) {
                if (endpoint.getWriter() != null) {
                    if (application.equals(endpoint.getWriter().getApplication()) && !endpoint.getWriter().isMissing()) {
                        return false;
                    }
                }
                for (EventChainItem item : endpoint.getReaders()) {
                    if (application.equals(item.getApplication()) && !item.isMissing()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public EventChainEvent findResponse(String id) {
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
    public Float calculateEdgePercentageFromEndpointToItem(EventChainEvent event, EventChainEndpoint endpoint, EventChainItem item) {
        if (endpoint.getWriter() == null) {
            return null;
        }
        if (item.isMissing()) {
            return null;
        }
        long endpointTime = item.getHandlingTime() - endpoint.getWriter().getHandlingTime();
        if (endpointTime <= 0) {
            return 0f;
        }
        // We found the time spent on the endpoint. Now find the root request to
        // calculate the percentage of time spend in the endpoint. We can't use
        // the the total event time, because this item can be in an async branch.
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
        float percentage = (float) endpointTime / (float) responseTime;
        return percentage < 0.001f ? 0.001f : percentage;
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
        float percentage = (float) handlingTime / (float) responseTime;
        return percentage < 0.01f ? 0.01f : percentage;
    }

    private EventChainEvent findRootRequest(EventChainEvent event) {
        return findRootRequest(event, new ArrayList<>());
    }

    private EventChainEvent findRootRequest(EventChainEvent event, List<String> inspectedEventIds) {
        if (inspectedEventIds.contains(event.getEventId())) {
            // We've found a cyclic reference. Abort the mission.
            return null;
        }
        inspectedEventIds.add(event.getEventId());
        if (event.isResponse()) {
            if (event.getCorrelationId() == null) {
                return null;
            }
            EventChainEvent request = findRequest(event.getCorrelationId());
            if (request == null) {
                return null;
            }
            return findRootRequest(request, inspectedEventIds);
        }
        EventChainEndpoint endpoint = event.getEndpoints().get(0);
        if (endpoint.getWriter() != null && endpoint.getWriter().getTransactionId() != null) {
            EventChainTransaction transaction = this.transactions.get(endpoint.getWriter().getTransactionId());
            if (!transaction.getReaders().isEmpty()) {
                EventChainItem eventChainItem = transaction.getReaders().get(0);
                EventChainEvent parent = this.events.get(eventChainItem.getEventId());
                if (!parent.isAsync()) {
                    return findRootRequest(parent, inspectedEventIds);
                }
            }
        }
        if (event.isRequest()) {
            return event;
        }
        return null;
    }


}
