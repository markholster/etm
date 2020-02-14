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

package com.jecstar.etm.domain.writer.json;

import com.jecstar.etm.domain.*;
import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.TelemetryEventWriter;

import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Converter class that converts a <code>TelemetryEvent</code> to a JSON string.
 *
 * @author mark
 */
public abstract class AbstractJsonTelemetryEventWriter<Event extends TelemetryEvent<Event>> implements TelemetryEventWriter<String, Event> {

    private final TelemetryEventTags tags = new TelemetryEventTagsJsonImpl();

    @Override
    public String write(Event event) {
        return write(event, true, true);
    }

    protected String write(Event event, boolean includeId, boolean includePayloadEncoding) {
        var builder = new JsonBuilder();
        builder.startObject();
        builder.field(this.tags.getObjectTypeTag(), getType());
        if (includeId) {
            builder.field(this.tags.getIdTag(), event.id);
        }
        builder.field(this.tags.getCorrelationIdTag(), event.correlationId);
        addMapElementToJsonBuilder(this.tags.getCorrelationDataTag(), event.correlationData, builder);
        if (event.endpoints.size() != 0) {
            builder.startArray(getTags().getEndpointsTag());
            for (var endpoint : event.endpoints) {
                addEndpointToJsonBuilder(endpoint, builder, this.tags);
            }
            builder.endArray();
        }
        if (!event.extractedData.isEmpty()) {
            builder.startObject(this.tags.getCorrelationDataTag());
            for (Entry<String, Object> entry : event.extractedData.entrySet()) {
                builder.fieldAsString(entry.getKey(), entry.getValue());
            }
            builder.endObject();
        }
        addMapElementToJsonBuilder(this.tags.getExtractedDataTag(), event.extractedData, builder);
        builder.field(this.tags.getNameTag(), event.name);
        addMapElementToJsonBuilder(this.tags.getMetadataTag(), event.metadata, builder);
        builder.field(this.tags.getPayloadTag(), event.payload);
        if (event.payloadEncoding != null) {
            builder.field(this.tags.getPayloadEncodingTag(), event.payloadEncoding.name());
        }
        if (event.payloadFormat != null) {
            builder.field(this.tags.getPayloadFormatTag(), event.payloadFormat.name());
        }
        if (event.payload != null) {
            builder.field(this.tags.getPayloadLengthTag(), event.payload.length());
        }
        doWrite(event, builder);
        builder.endObject();
        return builder.build();
    }

    abstract String getType();

    abstract void doWrite(Event event, JsonBuilder builder);

    private void addMapElementToJsonBuilder(String elementName, Map<String, Object> elementValues, JsonBuilder builder) {
        if (elementValues.size() < 1) {
            return;
        }
        builder.startObject(elementName);
        for (Entry<String, Object> entry : elementValues.entrySet()) {
            builder.fieldAsString(entry.getKey(), entry.getValue());
        }
        builder.endObject();
    }

    private void addEndpointToJsonBuilder(Endpoint endpoint, JsonBuilder builder, TelemetryEventTags tags) {
        boolean endpointHandlerSet = false;
        for (EndpointHandler handler : endpoint.getEndpointHandlers()) {
            if (handler.isSet()) {
                endpointHandlerSet = true;
                break;
            }
        }
        if (endpoint.name == null && !endpointHandlerSet) {
            return;
        }

        builder.startObject();
        builder.field(tags.getEndpointNameTag(), endpoint.name);
        if (endpoint.protocolType != null) {
            builder.field(tags.getEndpointProtocolTag(), endpoint.protocolType.name());
        }
        if (endpointHandlerSet) {
            builder.startArray(getTags().getEndpointHandlersTag());
            EndpointHandler writingEndpointHandler = endpoint.getWritingEndpointHandler();
            Instant latencyStart = null;
            if (writingEndpointHandler != null) {
                addEndpointHandlerToJsonBuilder(null, writingEndpointHandler, builder, getTags());
                latencyStart = writingEndpointHandler.handlingTime;
            }

            for (EndpointHandler endpointHandler : endpoint.getReadingEndpointHandlers()) {
                addEndpointHandlerToJsonBuilder(latencyStart, endpointHandler, builder, getTags());
            }
            builder.endArray();
        }
        builder.endObject();
    }

    private void addEndpointHandlerToJsonBuilder(Instant latencyStart, EndpointHandler endpointHandler, JsonBuilder builder, TelemetryEventTags tags) {
        if (!endpointHandler.isSet()) {
            return;
        }
        builder.startObject();
        builder.field(tags.getEndpointHandlerTypeTag(), endpointHandler.type.name());
        if (endpointHandler.handlingTime != null) {
            builder.field(tags.getEndpointHandlerHandlingTimeTag(), endpointHandler.handlingTime);
            if (latencyStart != null) {
                builder.field(tags.getEndpointHandlerLatencyTag(), endpointHandler.handlingTime.toEpochMilli() - latencyStart.toEpochMilli());
            }
        }
        builder.field(tags.getEndpointHandlerTransactionIdTag(), endpointHandler.transactionId);
        builder.field(tags.getEndpointHandlerSequenceNumberTag(), endpointHandler.sequenceNumber);
        addMapElementToJsonBuilder(tags.getMetadataTag(), endpointHandler.metadata, builder);
        Application application = endpointHandler.application;
        if (application.isSet()) {
            builder.startObject(tags.getEndpointHandlerApplicationTag());
            builder.field(tags.getApplicationNameTag(), application.name);
            builder.field(tags.getApplicationHostAddressTag(), tags.getApplicationHostNameTag(), application.hostAddress);
            builder.field(tags.getApplicationInstanceTag(), application.instance);
            builder.field(tags.getApplicationVersionTag(), application.version);
            builder.field(tags.getApplicationPrincipalTag(), application.principal);
            builder.endObject();
        }
        Location location = endpointHandler.location;
        if (location.isSet()) {
            builder.startObject(tags.getEndpointHandlerLocationTag());
            builder.field(tags.getLatitudeTag(), location.latitude);
            builder.field(tags.getLongitudeTag(), location.longitude);
            builder.endObject();
        }
        builder.endObject();
    }

    @Override
    public TelemetryEventTags getTags() {
        return this.tags;
    }
}
