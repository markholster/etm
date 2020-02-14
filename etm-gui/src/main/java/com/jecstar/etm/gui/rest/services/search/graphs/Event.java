/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.gui.rest.services.search.graphs;

import com.jecstar.etm.domain.writer.json.JsonBuilder;

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
    protected void doWriteToJson(JsonBuilder builder) {
        builder.field("name", getName());
        builder.field("event_id", getEventId());
        builder.field("correlation_event_id", getCorrelationEventId());
        builder.field("event_start_time", getEventStartTime());
        builder.field("event_end_time", getEventEndTime());
        builder.field("transaction_id", getTransactionId());
        builder.field("endpoint", getEndpointName());
        builder.field("async", isAsync());
        builder.field("order", getOrder());
        builder.field("response", isResponse());
        builder.field("sent", isSent());
        if (getAbsoluteTransactionPercentage() != null) {
            builder.field("absolute_event_percentage", getAbsoluteTransactionPercentage().doubleValue());
        }
        if (getAbsoluteDuration() != null) {
            builder.field("absolute_duration", getAbsoluteDuration().toMillis());
        }
    }

}
