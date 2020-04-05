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

    String getNodeNameTag();

    String getLicenseTag();

    String getSessionTimeoutTag();

    String getImportProfileCacheSizeTag();

    String getEnhancingHandlerCountTag();

    String getPersistingHandlerCountTag();

    String getEventBufferSizeTag();

    String getWaitStrategyTag();

    String getPersistingBulkCountTag();

    String getPersistingBulkSizeTag();

    String getPersistingBulkTimeTag();

    String getPersistingBulkThreadsTag();

    String getShardsPerIndexTag();

    String getReplicasPerIndexTag();

    String getMaxEventIndexCountTag();

    String getMaxMetricsIndexCountTag();

    String getMaxAuditLogIndexCountTag();

    String getWaitForActiveShardsTag();

    String getRetryOnConflictCountTag();

    String getQueryTimeoutTag();

    String getRemoteClustersTag();

    String getRemoteClusterNameTag();

    String getRemoteClusterClusterWideTag();

    String getRemoteClusterSeedsTag();

    String getRemoteClusterSeedHostTag();

    String getRemoteClusterSeedPortTag();

    String getMaxSearchResultDownloadRowsTag();

    String getMaxSearchHistoryCountTag();

    String getMaxSearchTemplateCountTag();

    String getMaxGraphCountTag();

    String getMaxDashboardCountTag();

    String getMaxSignalCountTag();

    // Search history tags
    String getSearchHistoryTag();

    String getTimestampTag();

    String getQueryTag();

    String getTypesTag();

    String getFieldsTag();

    String getResultsPerPageTag();

    String getSortFieldTag();

    String getSortOrderTag();

    // Node instances tags
    String getInstancesTag();

    String getLastSeenTag();
}
