package com.jecstar.etm.core.domain.converter;

public interface TelemetryEventConverterTags {

	// TelemetryEvent attributes.
	String getIdTag();
	String getCorrelationIdTag();
	String getCorrelationDataTag();
	String getExtractedDataTag();
	String getNameTag();
	String getMetadataTag();
	String getPackagingTag();
	String getPayloadTag();
	String getPayloadFormatTag();
	String getResponseTimeTag();
	String getResponseHandlingTimeTag();
	String getTransportTag();
	String getReadingEndpointHandlerTag();
	String getReadingEndpointHandlersTag();
	String getWritingEndpointHandlerTag();
	
	// Endpoint attributes;
	String getEndpointsTag();
	String getEndpointNameTag();

	// EndpointHandler attributes
	String getEndpointHandlerHandlingTimeTag();
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
