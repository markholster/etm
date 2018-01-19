package com.jecstar.etm.domain.writer;

public interface TelemetryEventTags {

    String EVENT_OBJECT_TYPE_BUSINESS = "business";
	String EVENT_OBJECT_TYPE_HTTP = "http";
    String EVENT_OBJECT_TYPE_LOG = "log";
    String EVENT_OBJECT_TYPE_MESSAGING = "messaging";
    String EVENT_OBJECT_TYPE_SQL = "sql";

    String PAYLOAD_ENCODING_BASE64 = "base64";
	String PAYLOAD_ENCODING_BASE64_CA_API_GATEWAY = "base64CaApiGateway";

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
	String getPayloadEncoding();
	String getPayloadFormatTag();
	String getPayloadLengthTag();
	String getReadingEndpointHandlerTag();
	String getReadingEndpointHandlersTag();
	String getWritingEndpointHandlerTag();
	
	// Endpoint attributes;
	String getEndpointsTag();
	String getEndpointNameTag();

	// EndpointHandler attributes
	String getEndpointHandlerHandlingTimeTag();
	String getEndpointHandlerLatencyTag();
	String getEndpointHandlerResponseTimeTag();
	String getEndpointHandlerTransactionIdTag();
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
	
	// Sql event attributes
	String getSqlEventTypeTag();
}
