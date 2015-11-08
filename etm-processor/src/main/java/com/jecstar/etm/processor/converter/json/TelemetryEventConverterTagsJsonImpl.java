package com.jecstar.etm.processor.converter.json;

import com.jecstar.etm.processor.converter.TelemetryEventConverterTags;

public class TelemetryEventConverterTagsJsonImpl implements TelemetryEventConverterTags {
	
	@Override
	public String getIdTag() {
		return "id";
	}

	@Override
	public String getApplicationTag() {
		return "application";
	}

	@Override
	public String getContentTag() {
		return "content";
	}

	@Override
	public String getEndpointTag() {
		return "endpoint";
	}

	@Override
	public String getExpiryTimeTag() {
		return "expiry_time";
	}

	@Override
	public String getCorrelationIdTag() {
		return "correlation_id";
	}
	
	@Override
	public String getChildCorrelationIdsTag() {
		return "child_correlation_ids";
	}
	
	@Override
	public String getCorrelationDataTag() {
		return "correlation_data";
	}

	@Override
	public String getCreationTimeTag() {
		return "creation_time";
	}

	@Override
	public String getDirectionTag() {
		return "direction";
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
	public String getResponsetimeTag() {
		return "responsetime";
	}

	@Override
	public String getTransactionIdTag() {
		return "transaction_id";
	}

	@Override
	public String getTransactionNameTag() {
		return "transaction_name";
	}

	@Override
	public String getTypeTag() {
		return "type";
	}

	@Override
	public String getRetentionTag() {
		return "retention";
	}
}
