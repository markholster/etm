package com.jecstar.etm.processor.elastic;

import com.jecstar.etm.core.domain.converter.EtmConfigurationConverterTags;

public class EtmConfigurationConverterTagsElasticImpl implements EtmConfigurationConverterTags {

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
	public String getPersistingBulkSizeTag() {
		return "persisting_bulk_size";
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
