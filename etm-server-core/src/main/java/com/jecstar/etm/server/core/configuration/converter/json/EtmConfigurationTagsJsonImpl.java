package com.jecstar.etm.server.core.configuration.converter.json;

import com.jecstar.etm.server.core.configuration.converter.EtmConfigurationTags;

public class EtmConfigurationTagsJsonImpl implements EtmConfigurationTags {

	@Override
	public String getNodeNameTag() {
		return "name";
	}
	
	@Override
	public String getLicenseTag() {
		return "license";
	}
	
	@Override
	public String getSessionTimeoutTag() {
		return "session_timeout";
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
	public String getWaitStrategyTag() {
		return "wait_strategy";
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
	public String getMaxEventIndexCountTag() {
		return "max_event_index_count";
	}
	
	@Override
	public String getMaxMetricsIndexCountTag() {
		return "max_metrics_index_count";
	}

	@Override
	public String getMaxAuditLogIndexCountTag() {
		return "max_audit_log_index_count";
	}
	
	@Override
	public String getWaitForActiveShardsTag() {
		return "wait_for_active_shards";
	}

	@Override
	public String getQueryTimeoutTag() {
		return "query_timeout";
	}

	@Override
	public String getRetryOnConflictCountTag() {
		return "retry_on_conflict_count";
	}
	
	@Override
	public String getMaxSearchResultDownloadRowsTag() {
		return "max_search_result_download_rows";
	}

	@Override
	public String getMaxSearchHistoryCountTag() {
		return "max_search_history_count";
	}

	@Override
	public String getMaxSearchTemplateCountTag() {
		return "max_search_template_count";
	}
	
	@Override
	public String getMaxGraphCountTag() {
		return "max_graph_count";
	}
	
	@Override
	public String getMaxDashboardCountTag() {
		return "max_dashboard_count";
	}

}
