/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.server.core.domain.configuration.converter;

public interface EtmConfigurationTags {

    default String getNodeNameTag() {
        return "name";
    }

    default String getSecretHashTag() {
        return "secret_hash";
    }

    default String getLicenseTag() {
        return "license";
    }

    default String getSessionTimeoutTag() {
        return "session_timeout";
    }

    default String getImportProfileCacheSizeTag() {
        return "import_profile_cache_size";
    }

    default String getEnhancingHandlerCountTag() {
        return "enhancing_handler_count";
    }

    default String getPersistingHandlerCountTag() {
        return "persisting_handler_count";
    }

    default String getEventBufferSizeTag() {
        return "event_buffer_size";
    }

    default String getWaitStrategyTag() {
        return "wait_strategy";
    }

    default String getPersistingBulkCountTag() {
        return "persisting_bulk_count";
    }

    default String getPersistingBulkSizeTag() {
        return "persisting_bulk_size";
    }

    default String getPersistingBulkTimeTag() {
        return "persisting_bulk_time";
    }

    default String getPersistingBulkThreadsTag() {
        return "persisting_bulk_threads";
    }

    default String getShardsPerIndexTag() {
        return "shards_per_index";
    }

    default String getReplicasPerIndexTag() {
        return "replicas_per_index";
    }

    default String getMaxEventIndexCountTag() {
        return "max_event_index_count";
    }

    default String getMaxMetricsIndexCountTag() {
        return "max_metrics_index_count";
    }

    default String getMaxAuditLogIndexCountTag() {
        return "max_audit_log_index_count";
    }

    default String getWaitForActiveShardsTag() {
        return "wait_for_active_shards";
    }

    default String getRetryOnConflictCountTag() {
        return "retry_on_conflict_count";
    }

    default String getQueryTimeoutTag() {
        return "query_timeout";
    }

    default String getRemoteClustersTag() {
        return "remote_clusters";
    }

    default String getRemoteClusterNameTag() {
        return "name";
    }

    default String getRemoteClusterClusterWideTag() {
        return "cluster_wide";
    }

    default String getRemoteClusterSeedsTag() {
        return "seeds";
    }

    default String getRemoteClusterSeedHostTag() {
        return "host";
    }

    default String getRemoteClusterSeedPortTag() {
        return "port";
    }

    default String getMaxSearchResultDownloadRowsTag() {
        return "max_search_result_download_rows";
    }

    default String getMaxSearchHistoryCountTag() {
        return "max_search_history_count";
    }

    default String getMaxSearchTemplateCountTag() {
        return "max_search_template_count";
    }

    default String getMaxGraphCountTag() {
        return "max_graph_count";
    }

    default String getMaxDashboardCountTag() {
        return "max_dashboard_count";
    }

    default String getMaxSignalCountTag() {
        return "max_signal_count";
    }

    default String getSearchHistoryTag() {
        return "search_history";
    }

    // Node instances tags
    default String getInstancesTag() {
        return "instances";
    }

    default String getLastSeenTag() {
        return "last_seen";
    }
}
