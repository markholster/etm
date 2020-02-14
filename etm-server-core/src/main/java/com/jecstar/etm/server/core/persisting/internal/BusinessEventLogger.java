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

package com.jecstar.etm.server.core.persisting.internal;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.PayloadFormat;
import com.jecstar.etm.domain.builder.BusinessTelemetryEventBuilder;
import com.jecstar.etm.domain.builder.EndpointBuilder;
import com.jecstar.etm.domain.writer.json.JsonBuilder;

public class BusinessEventLogger {

    private static final String BUSINESS_EVENT_ETM_STARTED = "{\"component\": \"etm\", \"node\": {0}, \"action\": \"started\"}";
    private static final String BUSINESS_EVENT_ETM_STOPPED = "{\"component\": \"etm\", \"node\": {0}, \"action\": \"stopped\"}";
    private static final String BUSINESS_EVENT_IBM_MQ_PROCESSOR_EMERGENCY_SHUTDOWN = "{\"component\": \"ibm mq processor\", \"node\": {0}, \"action\": \"emergency shutdown\", \"reason\": {1}}";
    private static final String BUSINESS_EVENT_REMOVED_INDEX = "{\"component\": \"index cleaner\", \"node\": {0}, \"action\": \"removed index\", \"index\": {1}}";
    private static final String BUSINESS_EVENT_LICENSE_EXPIRED = "{\"component\": \"etm\", \"action\": \"license expired\"}";
    private static final String BUSINESS_EVENT_LICENSE_NOT_YET_VALID = "{\"component\": \"etm\", \"action\": \"license not yet valid\"}";
    private static final String BUSINESS_EVENT_LICENSE_STORAGE_SIZE_EXCEEDED = "{\"component\": \"etm\", \"action\": \"license storage size exceeded\"}";
    private static final String BUSINESS_EVENT_SNMP_ENGINE_ID_ASSIGNMENT = "{\"component\": \"signaler\", \"node\": {0}, \"action\": \"SNMP engine ID assignment\", \"engineId\" : {1}}";
    private static final String BUSINESS_EVENT_SIGNAL_THRESHOLD_EXCEEDED = "{\"component\": \"signaler\", \"action\": \"signal threshold exceeded\", \"details\" : {0}}";
    private static final String BUSINESS_EVENT_SIGNAL_THRESHOLD_NO_LONGER_EXCEEDED = "{\"component\": \"signaler\", \"action\": \"signal threshold no longer exceeded\", \"details\" : {0}}";


    private static InternalBulkProcessorWrapper internalBulkProcessorWrapper;
    private static EndpointBuilder etmEndpoint;

    public static void initialize(InternalBulkProcessorWrapper bulkProcessorWrapper, EndpointBuilder etmEndpoint) {
        BusinessEventLogger.internalBulkProcessorWrapper = bulkProcessorWrapper;
        BusinessEventLogger.etmEndpoint = etmEndpoint;
    }

    public static void logEtmStartup() {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_ETM_STARTED
                        .replace("{0}", JsonBuilder.escapeToJson(etmEndpoint.getName(), true))
                )
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("Enterprise Telemetry Monitor node started")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logEtmShutdown() {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_ETM_STOPPED
                        .replace("{0}", JsonBuilder.escapeToJson(etmEndpoint.getName(), true))
                )
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("Enterprise Telemetry Monitor node stopped")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logMqProcessorEmergencyShutdown(Error e) {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_IBM_MQ_PROCESSOR_EMERGENCY_SHUTDOWN
                        .replace("{0}", JsonBuilder.escapeToJson(etmEndpoint.getName(), true))
                        .replace("{1}", JsonBuilder.escapeToJson(e.getMessage(), true))
                )
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("IBM MQ processor emergency shutdown")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logIndexRemoval(String indexName) {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_REMOVED_INDEX
                        .replace("{0}", JsonBuilder.escapeToJson(etmEndpoint.getName(), true))
                        .replace("{1}", JsonBuilder.escapeToJson(indexName, true))
                )
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("Index removed")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logLicenseExpired() {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_LICENSE_EXPIRED)
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("License expired")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logLicenseNotYetValid() {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_LICENSE_NOT_YET_VALID)
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("License not yet valid")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logLicenseSizeExceeded() {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_LICENSE_STORAGE_SIZE_EXCEEDED)
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("License storage size exceeded")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logSnmpEngineIdAssignment(String engineId) {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_SNMP_ENGINE_ID_ASSIGNMENT
                        .replace("{0}", JsonBuilder.escapeToJson(etmEndpoint.getName(), true))
                        .replace("{1}", JsonBuilder.escapeToJson(engineId, true))
                )
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("SNMP engine id assigned")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logSignalThresholdExceeded(String jsonDetailObject) {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_SIGNAL_THRESHOLD_EXCEEDED
                        .replace("{0}", jsonDetailObject)
                )
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("Signal threshold exceeded")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logSignalThresholdNoLongerExceeded(String jsonDetailObject) {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_SIGNAL_THRESHOLD_NO_LONGER_EXCEEDED
                        .replace("{0}", jsonDetailObject)
                )
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("Signal threshold no longer exceeded")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }


}
