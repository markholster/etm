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

package com.jecstar.etm.domain.builder;

import com.jecstar.etm.domain.*;

import java.util.Map;

public abstract class TelemetryEventBuilder<Event extends TelemetryEvent<Event>, Builder extends TelemetryEventBuilder<Event, Builder>> {

    final Event event;

    TelemetryEventBuilder(Event event) {
        this.event = event;
    }

    public Event build() {
        return this.event;
    }

    @SuppressWarnings("unchecked")
    public Builder initialize() {
        this.event.initialize();
        return (Builder) this;
    }

    @SuppressWarnings("unchecked")
    public Builder setId(String id) {
        this.event.id = id;
        return (Builder) this;
    }

    public String getId() {
        return this.event.id;
    }

    @SuppressWarnings("unchecked")
    public Builder setCorrelationId(String correlationId) {
        this.event.correlationId = correlationId;
        return (Builder) this;
    }

    public String getCorrelationId() {
        return this.event.correlationId;
    }

    @SuppressWarnings("unchecked")
    public Builder setTraceId(String traceId) {
        this.event.traceId = traceId;
        return (Builder) this;
    }

    public String getTraceId() {
        return this.event.traceId;
    }

    @SuppressWarnings("unchecked")
    public Builder setCorrelationData(Map<String, Object> correlationData) {
        this.event.correlationData = correlationData;
        return (Builder) this;
    }

    @SuppressWarnings("unchecked")
    public Builder addCorrelationData(Map<String, Object> correlationData) {
        this.event.correlationData.putAll(correlationData);
        return (Builder) this;
    }

    @SuppressWarnings("unchecked")
    public Builder addCorrelationData(String key, Object value) {
        this.event.correlationData.put(key, value);
        return (Builder) this;
    }

    public Map<String, Object> getCorrelationData() {
        return this.event.correlationData;
    }

    @SuppressWarnings("unchecked")
    private Builder addOrMergeEndpoint(Endpoint endpoint) {
        int ix = this.event.endpoints.indexOf(endpoint);
        if (ix == -1) {
            this.event.endpoints.add(endpoint);
        } else {
            Endpoint currentEndpoint = this.event.endpoints.get(ix);
            for (EndpointHandler handler : endpoint.getEndpointHandlers()) {
                if (handler.isSet()) {
                    currentEndpoint.addEndpointHandler(handler);
                }
            }
        }
        return (Builder) this;
    }

    public Builder addOrMergeEndpoint(EndpointBuilder endpointBuilder) {
        return addOrMergeEndpoint(endpointBuilder.build());
    }

    @SuppressWarnings("unchecked")
    public Builder setExtractedData(Map<String, Object> extractedData) {
        this.event.extractedData = extractedData;
        return (Builder) this;
    }

    @SuppressWarnings("unchecked")
    public Builder addExtractedData(Map<String, Object> extractedData) {
        this.event.extractedData.putAll(extractedData);
        return (Builder) this;
    }

    @SuppressWarnings("unchecked")
    public Builder addExtractedData(String key, Object value) {
        this.event.extractedData.put(key, value);
        return (Builder) this;
    }

    @SuppressWarnings("unchecked")
    public Builder setName(String name) {
        this.event.name = name;
        return (Builder) this;
    }

    @SuppressWarnings("unchecked")
    public Builder setMetadata(Map<String, Object> metadata) {
        this.event.metadata = metadata;
        return (Builder) this;
    }

    @SuppressWarnings("unchecked")
    public Builder addMetadata(Map<String, Object> metadata) {
        this.event.metadata.putAll(metadata);
        return (Builder) this;
    }

    @SuppressWarnings("unchecked")
    public Builder addMetadata(String key, Object value) {
        this.event.metadata.put(key, value);
        return (Builder) this;
    }

    public Map<String, Object> getMetadata() {
        return this.event.metadata;
    }

    @SuppressWarnings("unchecked")
    public Builder setPayload(String payload) {
        this.event.payload = payload;
        return (Builder) this;
    }

    public String getPayload() {
        return this.event.payload;
    }

    @SuppressWarnings("unchecked")
    public Builder setPayloadEncoding(PayloadEncoding payloadEncoding) {
        this.event.payloadEncoding = payloadEncoding;
        return (Builder) this;
    }

    public PayloadEncoding getPayloadEncoding() {
        return this.event.payloadEncoding;
    }

    @SuppressWarnings("unchecked")
    public Builder setPayloadFormat(PayloadFormat payloadFormat) {
        this.event.payloadFormat = payloadFormat;
        return (Builder) this;
    }

    public PayloadFormat getPayloadFormat() {
        return this.event.payloadFormat;
    }
}
