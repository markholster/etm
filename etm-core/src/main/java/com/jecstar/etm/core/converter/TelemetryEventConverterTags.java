package com.jecstar.etm.core.converter;

public interface TelemetryEventConverterTags {
	
	// Map attributes.
	String getMapKeyTag();
	String getMapValueTag();
	
	// TelemetryEvent attributes.
	String getIdTag();
	String getApplicationTag();
	String getContentTag();
	String getContentLengthTag();
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
	
	// Correlation tags.
	String getChildCorrelationIdsTag();
	String getResponsetimeTag();
}
