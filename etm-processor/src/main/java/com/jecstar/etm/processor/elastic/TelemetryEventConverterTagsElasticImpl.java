package com.jecstar.etm.processor.elastic;

import com.jecstar.etm.core.domain.converter.TelemetryEventConverterTags;

public class TelemetryEventConverterTagsElasticImpl implements TelemetryEventConverterTags {

	@Override
	public String getIdTag() {
		return "id";
	}

	@Override
	public String getCorrelationIdTag() {
		return "correlation_id";
	}

	@Override
	public String getCorrelationDataTag() {
		return "correlation_data";
	}

	@Override
	public String getEndpointTag() {
		return "endpoint";
	}

	@Override
	public String getExpiryTag() {
		return "expiry";
	}

	@Override
	public String getExtractedDataTag() {
		return "extracted_data";
	}

	@Override
	public String getMetadataTag() {
		return "metadata";
	}

	@Override
	public String getNameTag() {
		return "name";
	}
	
	@Override
	public String getPackagingTag() {
		return "packaging";
	}

	@Override
	public String getPayloadTag() {
		return "payload";
	}

	@Override
	public String getPayloadFormatTag() {
		return "payload_format";
	}

	@Override
	public String getResponseTimeTag() {
		return "response_time";
	}
	
	@Override
	public String getResponseHandlingTimeTag() {
		return "response_handling_time";
	}

	@Override
	public String getReadingEndpointHandlersTag() {
		return "reading_endpoint_handlers";
	}
	
	@Override
	public String getTransportTag() {
		return "transport";
	}

	@Override
	public String getWritingEndpointHandlerTag() {
		return "writing_endpoint_handler";
	}

	@Override
	public String getEndpointHandlerHandlingTimeTag() {
		return "handling_time";
	}

	@Override
	public String getEndpointHandlerApplicationTag() {
		return "application";
	}

	@Override
	public String getApplicationNameTag() {
		return "name";
	}

	@Override
	public String getApplicationInstanceTag() {
		return "instance";
	}

	@Override
	public String getApplicationVersionTag() {
		return "version";
	}

	@Override
	public String getApplicationPrincipalTag() {
		return "principal";
	}

}
