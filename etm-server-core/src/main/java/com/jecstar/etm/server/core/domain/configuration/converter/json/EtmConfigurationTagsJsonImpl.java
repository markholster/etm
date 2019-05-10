package com.jecstar.etm.server.core.domain.configuration.converter.json;

import com.jecstar.etm.server.core.domain.configuration.converter.EtmConfigurationTags;

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
    public String getImportProfileCacheSizeTag() {
        return "import_profile_cache_size";
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
    public String getPersistingBulkThreadsTag() {
        return "persisting_bulk_threads";
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

    @Override
    public String getMaxSignalCountTag() {
        return "max_signal_count";
    }

    @Override
    public String getSearchHistoryTag() {
        return "search_history";
    }

    @Override
    public String getTimestampTag() {
        return "timestamp";
    }

    @Override
    public String getQueryTag() {
        return "query";
    }

    @Override
    public String getTypesTag() {
        return "types";
    }

    @Override
    public String getFieldsTag() {
        return "fields";
    }

    @Override
    public String getResultsPerPageTag() {
        return "results_per_page";
    }

    @Override
    public String getSortFieldTag() {
        return "sort_field";
    }

    @Override
    public String getSortOrderTag() {
        return "sort_order";
    }

    @Override
    public String getStartTimeTag() {
        return "start_time";
    }

    @Override
    public String getEndTimeTag() {
        return "end_time";
    }

    @Override
    public String getTimeFilterFieldTag() {
        return "time_filter_field";
    }
}
