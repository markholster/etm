package com.jecstar.etm.domain.writers.json;

import com.jecstar.etm.domain.writers.TelemetryEventTags;

public class TelemetryEventTagsJsonImpl implements TelemetryEventTags {

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
	public String getCorrelationsTag() {
		return "correlations";
	}

	@Override
	public String getEndpointsTag() {
		return "endpoints";
	}

	@Override
	public String getEndpointNameTag() {
		return "name";
	}

	@Override
	public String getEventHashesTag() {
		return "event_hashes";
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
	public String getReadingEndpointHandlerTag() {
		return "reading_endpoint_handler";
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
	public String getEndpointHandlerLatencyTag() {
		return "latency";
	}
	
	@Override
	public String getEndpointHandlerResponseTimeTag() {
		return "response_time";
	}
	
	@Override
	public String getEndpointHandlerTransactionIdTag() {
		return "transaction_id";
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
	public String getApplicationHostAddressTag() {
		return "host_address";
	}
	
	@Override
	public String getApplicationHostNameTag() {
		return "host_name";
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

	@Override
	public String getEndpointHandlerLocationTag() {
		return "location";
	}

	@Override
	public String getLatitudeTag() {
		return "lat";
	}

	@Override
	public String getLongitudeTag() {
		return "lon";
	}

	@Override
	public String getMessagingEventTypeTag() {
		return "messaging_type";
	}

	@Override
	public String getLogLevelTag() {
		return "log_level";
	}
	
	@Override
	public String getStackTraceTag() {
		return "stack_trace";
	}

	@Override
	public String getHttpEventTypeTag() {
		return "http_type";
	}

	@Override
	public String getSqlEventTypeTag() {
		return "sql_type";
	}
}
