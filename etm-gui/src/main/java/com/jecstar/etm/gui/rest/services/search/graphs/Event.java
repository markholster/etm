package com.jecstar.etm.gui.rest.services.search.graphs;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class Event extends AbstractVertex {

    private final String eventId;
    private final String name;
    private final Application parent;
    private String correlationEventId;
    private String transactionId;
    private Instant eventStartTime;
    private Instant eventEndTime;
    private boolean async;
    private int order;
    private boolean response;
    private boolean sent;
    private Duration absoluteDuration;
    private BigDecimal absoluteTransactionPercentage;
    private String endpointName;

    public Event(String eventId, String name) {
        this(eventId, name, (Application) null);
    }

    public Event(String eventId, String name, Application parent) {
        this(UUID.randomUUID().toString(), eventId, name, parent);
    }

    public Event(String vertexId, String eventId, String name) {
        this(vertexId, eventId, name, null);
    }

    public Event(String vertexId, String eventId, String name, Application parent) {
        super(vertexId);
        this.eventId = eventId;
        this.name = name;
        this.parent = parent;
    }

    public void calculateAbsoluteTransactionMetrics(Duration absoluteDuration, Duration totalTransactionDuration) {
        this.absoluteDuration = absoluteDuration;
        this.absoluteTransactionPercentage = new BigDecimal(Float.toString((float) absoluteDuration.toMillis() / (float) totalTransactionDuration.toMillis()));
        this.absoluteTransactionPercentage = this.absoluteTransactionPercentage.setScale(4, RoundingMode.HALF_UP);
    }

    public void resetAbsoluteTransactionMetrics() {
        this.absoluteDuration = null;
        this.absoluteTransactionPercentage = null;
    }

    public Duration getTotalEventTime() {
        if (this.eventStartTime == null || this.eventEndTime == null) {
            return null;
        }
        return Duration.ofMillis(this.eventEndTime.toEpochMilli() - this.eventStartTime.toEpochMilli());
    }

    public BigDecimal getAbsoluteTransactionPercentage() {
        return this.absoluteTransactionPercentage;
    }

    public Duration getAbsoluteDuration() {
        return this.absoluteDuration;
    }

    public String getEventId() {
        return this.eventId;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public Application getParent() {
        return this.parent;
    }

    public Instant getEventStartTime() {
        return this.eventStartTime;
    }

    public Event setEventStartTime(Instant eventStartTime) {
        this.eventStartTime = eventStartTime;
        return this;
    }

    public Instant getEventEndTime() {
        return this.eventEndTime;
    }

    public Event setEventEndTime(Instant eventEndTime) {
        this.eventEndTime = eventEndTime;
        return this;
    }

    public String getCorrelationEventId() {
        return this.correlationEventId;
    }

    public Event setCorrelationEventId(String correlationEventId) {
        this.correlationEventId = correlationEventId;
        return this;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public Event setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public boolean isAsync() {
        return this.async;
    }

    public Event setAsync(boolean async) {
        this.async = async;
        return this;
    }

    public int getOrder() {
        return this.order;
    }

    public Event setOrder(int order) {
        this.order = order;
        return this;
    }

    public boolean isResponse() {
        return this.response;
    }

    public Event setResponse(boolean response) {
        this.response = response;
        return this;
    }

    public boolean isSent() {
        return this.sent;
    }

    public boolean isReceived() {
        return !isSent();
    }

    public Event setSent(boolean sent) {
        this.sent = sent;
        return this;
    }

    public String getEndpointName() {
        return this.endpointName;
    }

    public Event setEndpointName(String endpointName) {
        this.endpointName = endpointName;
        return this;
    }

    @Override
    protected String getType() {
        return "event";
    }

    @Override
    protected void doWriteToJson(StringBuilder buffer) {
        jsonWriter.addStringElementToJsonBuffer("name", getName(), buffer, false);
        jsonWriter.addStringElementToJsonBuffer("event_id", getEventId(), buffer, false);
        jsonWriter.addStringElementToJsonBuffer("correlation_event_id", getCorrelationEventId(), buffer, false);
        jsonWriter.addInstantElementToJsonBuffer("event_start_time", getEventStartTime(), buffer, false);
        jsonWriter.addInstantElementToJsonBuffer("event_end_time", getEventEndTime(), buffer, false);
        jsonWriter.addStringElementToJsonBuffer("transaction_id", getTransactionId(), buffer, false);
        jsonWriter.addStringElementToJsonBuffer("endpoint", getEndpointName(), buffer, false);
        jsonWriter.addBooleanElementToJsonBuffer("async", isAsync(), buffer, false);
        jsonWriter.addIntegerElementToJsonBuffer("order", getOrder(), buffer, false);
        jsonWriter.addBooleanElementToJsonBuffer("response", isResponse(), buffer, false);
        jsonWriter.addBooleanElementToJsonBuffer("sent", isSent(), buffer, false);
        if (getAbsoluteTransactionPercentage() != null) {
            jsonWriter.addDoubleElementToJsonBuffer("absolute_event_percentage", getAbsoluteTransactionPercentage().doubleValue(), buffer, false);
        }
        if (getAbsoluteDuration() != null) {
            jsonWriter.addLongElementToJsonBuffer("absolute_duration", getAbsoluteDuration().toMillis(), buffer, false);
        }
    }


}
