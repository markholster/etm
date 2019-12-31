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
