package com.jecstar.etm.server.core.domain.converter.json;

import com.jecstar.etm.server.core.domain.converter.EtmConfigurationTags;

public class EtmConfigurationTagsJsonImpl implements EtmConfigurationTags {

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

	@Override
	public String getMaxIndexCountTag() {
		return "max_index_count";
	}

	@Override
	public String getWriteConsistencyTag() {
		return "write_consistency";
	}

	@Override
	public String getQueryTimeoutTag() {
		return "query_timeout";
	}

	@Override
	public String getRetryOnConflictCountTag() {
		return "retry_on_conflict_count";
	}

}
