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

package com.jecstar.etm.domain.writer;

public interface TelemetryEventTags {

    String EVENT_OBJECT_TYPE_BUSINESS = "business";
    String EVENT_OBJECT_TYPE_HTTP = "http";
    String EVENT_OBJECT_TYPE_LOG = "log";
    String EVENT_OBJECT_TYPE_MESSAGING = "messaging";
    String EVENT_OBJECT_TYPE_SQL = "sql";

    // TelemetryEvent attributes.
    String getTimestampTag();

    String getObjectTypeTag();

    String getIdTag();

    String getCorrelationIdTag();

    String getTraceIdTag();

    String getCorrelationDataTag();

    String getCorrelationsTag();

    String getExtractedDataTag();

    String getEventHashesTag();

    String getNameTag();

    String getMetadataTag();

    String getPayloadTag();

    String getPayloadEncodingTag();

    String getPayloadFormatTag();

    String getPayloadLengthTag();

    String getEndpointHandlersTag();

    // Endpoint attributes;
    String getEndpointsTag();

    String getEndpointNameTag();

    String getEndpointProtocolTag();

    // EndpointHandler attributes
    String getEndpointHandlerTypeTag();

    String getEndpointHandlerHandlingTimeTag();

    String getEndpointHandlerLatencyTag();

    String getEndpointHandlerResponseTimeTag();

    String getEndpointHandlerTransactionIdTag();

    String getEndpointHandlerSequenceNumberTag();

    String getEndpointHandlerApplicationTag();
    // Application attributes

    String getApplicationNameTag();

    String getApplicationHostAddressTag();

    String getApplicationHostNameTag();

    String getApplicationInstanceTag();

    String getApplicationVersionTag();

    String getApplicationPrincipalTag();
    // Location attributes.

    String getEndpointHandlerLocationTag();

    String getLatitudeTag();

    String getLongitudeTag();
    // Messaging event attributes

    String getExpiryTag();

    String getMessagingEventTypeTag();
    // Log event attributes

    String getLogLevelTag();

    String getStackTraceTag();
    // Http event attributes

    String getHttpEventTypeTag();

    String getStatusCodeTag();
    // Sql event attributes

    String getSqlEventTypeTag();
}
