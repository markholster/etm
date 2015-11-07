package com.jecstar.etm.processor.converter;

public interface TelemetryEventConverterTags {
	
	// TelemetryEvent attributes.
	String getIdTag();
	String getApplicationTag();
	String getContentTag();
	String getCorrelationIdTag();
	String getCorrelationDataTag();
	String getCreationTimeTag();
	String getDirectionTag();
	String getEndpointTag();
	String getExpiryTimeTag();
	String getMetadataTag();
	String getNameTag();
	String getTransactionIdTag();
	String getTransactionNameTag();
	String getTypeTag();
	
	// Retention attributes
	String getRetentionTag();
}
