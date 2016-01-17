package com.jecstar.etm.core.domain.converter.json;

import com.jecstar.etm.core.domain.converter.EtmConfigurationConverterTags;

public class EtmConfigurationConverterTagsJsonImpl implements EtmConfigurationConverterTags {

	@Override
	public String getLicenseTag() {
		return "license";
	}
	
	@Override
	public String getEnhancingHandlerCountTag() {
		return "enhancing_handler_count";
	}

	@Override
	public String getPersistingHandlerCountTag() {
		return "persisting_handler_count";
	}

	@Override
	public String getEventBufferSizeTag() {
		return "event_buffer_size";
	}

	@Override
	public String getPersistingBulkCountTag() {
		return "persisting_bulk_count";
	}

	@Override
	public String getPersistingBulkSizeTag() {
		return "persisting_bulk_size";
	}

	@Override
	public String getPersistingBulkTimeTag() {
		return "persisting_bulk_time";
	}

	@Override
	public String getShardsPerIndexTag() {
		return "shards_per_index";
	}

	@Override
	public String getReplicasPerIndexTag() {
		return "replicas_per_index";
	}

}
