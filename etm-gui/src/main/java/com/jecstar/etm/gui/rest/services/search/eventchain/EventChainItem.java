package com.jecstar.etm.gui.rest.services.search.eventchain;

import com.jecstar.etm.domain.HttpTelemetryEvent.HttpEventType;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;

public class EventChainItem {

    private final String eventId;
    private final String transactionId;
    private final long handlingTime;
    private String correlationId;
    private String name;
    private EventChainApplication application;
    private String eventType;
    private Long responseTime;
    private Long expiry;
    private String subType;
    private boolean missing;

    EventChainItem(String transactionId, String eventId, long handlingTime) {
        this.transactionId = transactionId;
        this.eventId = eventId;
        this.handlingTime = handlingTime;
    }

    public String getKey() {
        return this.transactionId + "_" + this.eventId;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public String getEventId() {
        return this.eventId;
    }

    public long getHandlingTime() {
        return this.handlingTime;
    }

    public EventChainItem setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    public String getCorrelationId() {
        return this.correlationId;
    }

    public EventChainItem setSubType(String subType) {
        this.subType = subType;
        return this;
    }

    public String getSubType() {
        return this.subType;
    }

    public boolean isRequest() {
        if ("messaging".equals(this.eventType)) {
            return MessagingEventType.REQUEST.name().equals(this.subType);
        } else if ("http".equals(this.eventType)) {
            return !HttpEventType.RESPONSE.name().equals(this.subType);
        }
        return false;
    }

    public boolean isResponse() {
        if ("messaging".equals(this.eventType)) {
            return MessagingEventType.RESPONSE.name().equals(this.subType);
        } else if ("http".equals(this.eventType)) {
            return HttpEventType.RESPONSE.name().equals(this.subType);
        }
        return false;
    }

    public boolean isAsync() {
        return "messaging".equals(this.eventType) && MessagingEventType.FIRE_FORGET.name().equals(this.subType);
    }

    public boolean isHttpEvent() {
        return "http".equals(this.eventType);
    }

    public boolean isMessagingEvent() {
        return "messaging".equals(this.eventType);
    }

    public EventChainItem setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public EventChainItem setApplication(String applicationName, String applicationInstance) {
        if (applicationName == null) {
            this.application = null;
            return this;
        }
        this.application = new EventChainApplication(applicationName, applicationInstance);
        return this;
    }

    public EventChainApplication getApplication() {
        return this.application;
    }

    public EventChainItem setEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public String getEventType() {
        return this.eventType;
    }

    public EventChainItem setResponseTime(Long responseTime) {
        this.responseTime = responseTime;
        return this;
    }

    public Long getResponseTime() {
        return this.responseTime;
    }

    public EventChainItem setExpiry(Long expiry) {
        this.expiry = expiry;
        return this;
    }

    public Long getExpiry() {
        return this.expiry;
    }

    public EventChainItem setMissing(boolean missing) {
        this.missing = missing;
        return this;
    }

    public boolean isMissing() {
        return this.missing;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EventChainItem) {
            EventChainItem other = (EventChainItem) obj;
            return getKey().equals(other.getKey());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }
}
