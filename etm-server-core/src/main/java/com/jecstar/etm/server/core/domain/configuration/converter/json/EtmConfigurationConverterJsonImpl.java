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

package com.jecstar.etm.server.core.domain.configuration.converter.json;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.configuration.RemoteCluster;
import com.jecstar.etm.server.core.domain.configuration.WaitStrategy;
import com.jecstar.etm.server.core.domain.configuration.converter.EtmConfigurationConverter;
import com.jecstar.etm.server.core.domain.configuration.converter.EtmConfigurationTags;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converter class that converts a <code>TelemetryEvent</code> to a JSON string.
 *
 * @author mark
 */
public class EtmConfigurationConverterJsonImpl implements EtmConfigurationConverter<String> {

    private final EtmConfigurationTags tags = new EtmConfigurationTagsJsonImpl();
    private final JsonConverter converter = new JsonConverter();

    @Override
    public String write(EtmConfiguration nodeConfiguration, EtmConfiguration defaultConfiguration) {
        final JsonBuilder builder = new JsonBuilder();
        builder.startObject();
        builder.field(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE);
        builder.startObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE);
        if (nodeConfiguration == null) {
            // only add the defaults.
            builder.field(this.tags.getNodeNameTag(), ElasticsearchLayout.ETM_OBJECT_NAME_DEFAULT);
            builder.field(this.tags.getSecretHashTag(), defaultConfiguration.getSecretHash());
            builder.field(this.tags.getSessionTimeoutTag(), defaultConfiguration.getSessionTimeout());
            builder.field(this.tags.getImportProfileCacheSizeTag(), defaultConfiguration.getImportProfileCacheSize());
            builder.field(this.tags.getEnhancingHandlerCountTag(), defaultConfiguration.getEnhancingHandlerCount());
            builder.field(this.tags.getPersistingHandlerCountTag(), defaultConfiguration.getPersistingHandlerCount());
            builder.field(this.tags.getEventBufferSizeTag(), defaultConfiguration.getEventBufferSize());
            builder.field(this.tags.getWaitStrategyTag(), defaultConfiguration.getWaitStrategy().name());
            builder.field(this.tags.getPersistingBulkCountTag(), defaultConfiguration.getPersistingBulkCount());
            builder.field(this.tags.getPersistingBulkSizeTag(), defaultConfiguration.getPersistingBulkSize());
            builder.field(this.tags.getPersistingBulkTimeTag(), defaultConfiguration.getPersistingBulkTime());
            builder.field(this.tags.getPersistingBulkThreadsTag(), defaultConfiguration.getPersistingBulkThreads());
            builder.field(this.tags.getShardsPerIndexTag(), defaultConfiguration.getShardsPerIndex());
            builder.field(this.tags.getReplicasPerIndexTag(), defaultConfiguration.getReplicasPerIndex());
            builder.field(this.tags.getMaxEventIndexCountTag(), defaultConfiguration.getMaxEventIndexCount());
            builder.field(this.tags.getMaxMetricsIndexCountTag(), defaultConfiguration.getMaxMetricsIndexCount());
            builder.field(this.tags.getMaxAuditLogIndexCountTag(), defaultConfiguration.getMaxAuditLogIndexCount());
            builder.field(this.tags.getMaxSearchResultDownloadRowsTag(), defaultConfiguration.getMaxSearchResultDownloadRows());
            builder.field(this.tags.getMaxSearchHistoryCountTag(), defaultConfiguration.getMaxSearchHistoryCount());
            builder.field(this.tags.getMaxSearchTemplateCountTag(), defaultConfiguration.getMaxSearchTemplateCount());
            builder.field(this.tags.getMaxGraphCountTag(), defaultConfiguration.getMaxGraphCount());
            builder.field(this.tags.getMaxDashboardCountTag(), defaultConfiguration.getMaxDashboardCount());
            builder.field(this.tags.getMaxSignalCountTag(), defaultConfiguration.getMaxSignalCount());
            builder.field(this.tags.getWaitForActiveShardsTag(), defaultConfiguration.getWaitForActiveShards());
            builder.field(this.tags.getQueryTimeoutTag(), defaultConfiguration.getQueryTimeout());
            builder.field(this.tags.getRetryOnConflictCountTag(), defaultConfiguration.getRetryOnConflictCount());
            builder.startArray(this.tags.getRemoteClustersTag());
            for (var remoteCLuster : defaultConfiguration.getRemoteClusters()) {
                builder.startObject();
                builder.field(this.getTags().getRemoteClusterNameTag(), remoteCLuster.getName());
                builder.field(this.tags.getRemoteClusterClusterWideTag(), remoteCLuster.isClusterWide());
                builder.startArray(this.tags.getRemoteClusterSeedsTag());
                for (var seed : remoteCLuster.getSeeds()) {
                    builder.startObject();
                    builder.field(this.tags.getRemoteClusterSeedHostTag(), seed.getHost());
                    builder.field(this.tags.getRemoteClusterSeedPortTag(), seed.getPort());
                    builder.endObject();
                }
                builder.endArray();
                builder.endObject();
            }
            builder.endArray();
        } else {
            builder.field(this.tags.getNodeNameTag(), nodeConfiguration.getNodeName());
            addLongWhenNotDefault(this.tags.getSessionTimeoutTag(), defaultConfiguration.getSessionTimeout(), nodeConfiguration.getSessionTimeout(), builder);
            addIntegerWhenNotDefault(this.tags.getImportProfileCacheSizeTag(), defaultConfiguration.getImportProfileCacheSize(), nodeConfiguration.getImportProfileCacheSize(), builder);
            addIntegerWhenNotDefault(this.tags.getEnhancingHandlerCountTag(), defaultConfiguration.getEnhancingHandlerCount(), nodeConfiguration.getEnhancingHandlerCount(), builder);
            addIntegerWhenNotDefault(this.tags.getPersistingHandlerCountTag(), defaultConfiguration.getPersistingHandlerCount(), nodeConfiguration.getPersistingHandlerCount(), builder);
            addIntegerWhenNotDefault(this.tags.getEventBufferSizeTag(), defaultConfiguration.getEventBufferSize(), nodeConfiguration.getEventBufferSize(), builder);
            addStringWhenNotDefault(this.tags.getWaitStrategyTag(), defaultConfiguration.getWaitStrategy().name(), nodeConfiguration.getWaitStrategy().name(), builder);
            addIntegerWhenNotDefault(this.tags.getPersistingBulkCountTag(), defaultConfiguration.getPersistingBulkCount(), nodeConfiguration.getPersistingBulkCount(), builder);
            addIntegerWhenNotDefault(this.tags.getPersistingBulkSizeTag(), defaultConfiguration.getPersistingBulkSize(), nodeConfiguration.getPersistingBulkSize(), builder);
            addIntegerWhenNotDefault(this.tags.getPersistingBulkTimeTag(), defaultConfiguration.getPersistingBulkTime(), nodeConfiguration.getPersistingBulkTime(), builder);
            addIntegerWhenNotDefault(this.tags.getPersistingBulkThreadsTag(), defaultConfiguration.getPersistingBulkThreads(), nodeConfiguration.getPersistingBulkThreads(), builder);
            addIntegerWhenNotDefault(this.tags.getShardsPerIndexTag(), defaultConfiguration.getShardsPerIndex(), nodeConfiguration.getShardsPerIndex(), builder);
            addIntegerWhenNotDefault(this.tags.getReplicasPerIndexTag(), defaultConfiguration.getReplicasPerIndex(), nodeConfiguration.getReplicasPerIndex(), builder);
            addIntegerWhenNotDefault(this.tags.getMaxEventIndexCountTag(), defaultConfiguration.getMaxEventIndexCount(), nodeConfiguration.getMaxEventIndexCount(), builder);
            addIntegerWhenNotDefault(this.tags.getMaxMetricsIndexCountTag(), defaultConfiguration.getMaxMetricsIndexCount(), nodeConfiguration.getMaxMetricsIndexCount(), builder);
            addIntegerWhenNotDefault(this.tags.getMaxAuditLogIndexCountTag(), defaultConfiguration.getMaxAuditLogIndexCount(), nodeConfiguration.getMaxAuditLogIndexCount(), builder);
            addIntegerWhenNotDefault(this.tags.getMaxSearchResultDownloadRowsTag(), defaultConfiguration.getMaxSearchResultDownloadRows(), nodeConfiguration.getMaxSearchResultDownloadRows(), builder);
            addIntegerWhenNotDefault(this.tags.getMaxSearchHistoryCountTag(), defaultConfiguration.getMaxSearchHistoryCount(), nodeConfiguration.getMaxSearchHistoryCount(), builder);
            addIntegerWhenNotDefault(this.tags.getMaxSearchTemplateCountTag(), defaultConfiguration.getMaxSearchTemplateCount(), nodeConfiguration.getMaxSearchTemplateCount(), builder);
            addIntegerWhenNotDefault(this.tags.getMaxGraphCountTag(), defaultConfiguration.getMaxGraphCount(), nodeConfiguration.getMaxGraphCount(), builder);
            addIntegerWhenNotDefault(this.tags.getMaxDashboardCountTag(), defaultConfiguration.getMaxDashboardCount(), nodeConfiguration.getMaxDashboardCount(), builder);
            addIntegerWhenNotDefault(this.tags.getMaxSignalCountTag(), defaultConfiguration.getMaxSignalCount(), nodeConfiguration.getMaxSignalCount(), builder);
            addIntegerWhenNotDefault(this.tags.getWaitForActiveShardsTag(), defaultConfiguration.getWaitForActiveShards(), nodeConfiguration.getWaitForActiveShards(), builder);
            addLongWhenNotDefault(this.tags.getQueryTimeoutTag(), defaultConfiguration.getQueryTimeout(), nodeConfiguration.getQueryTimeout(), builder);
            addIntegerWhenNotDefault(this.tags.getRetryOnConflictCountTag(), defaultConfiguration.getRetryOnConflictCount(), nodeConfiguration.getRetryOnConflictCount(), builder);
        }
        builder.endObject().endObject();
        return builder.build();
    }

    @Override
    public EtmConfiguration read(String nodeJsonContent, String defaultJsonContent, String nodeName) {
        Map<String, Object> nodeMap = nodeJsonContent == null ? null : this.converter.toMap(nodeJsonContent);
        Map<String, Object> defaultMap = this.converter.toMap(defaultJsonContent);
        return read(nodeMap, defaultMap, nodeName);
    }

    public EtmConfiguration read(Map<String, Object> nodeMap, Map<String, Object> defaultMap, String nodeName) {
        nodeMap = this.converter.getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE, nodeMap);
        defaultMap = this.converter.getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE, defaultMap);
        EtmConfiguration etmConfiguration = new EtmConfiguration(nodeName);
        etmConfiguration.setSecretHash(getStringValue(this.tags.getSecretHashTag(), defaultMap, nodeMap));
        etmConfiguration.setSessionTimeout(getLongValue(this.tags.getSessionTimeoutTag(), defaultMap, nodeMap));
        etmConfiguration.setImportProfileCacheSize(getIntValue(this.tags.getImportProfileCacheSizeTag(), defaultMap, nodeMap));
        etmConfiguration.setEnhancingHandlerCount(getIntValue(this.tags.getEnhancingHandlerCountTag(), defaultMap, nodeMap));
        etmConfiguration.setPersistingHandlerCount(getIntValue(this.tags.getPersistingHandlerCountTag(), defaultMap, nodeMap));
        etmConfiguration.setEventBufferSize(getIntValue(this.tags.getEventBufferSizeTag(), defaultMap, nodeMap));
        etmConfiguration.setWaitStrategy(WaitStrategy.safeValueOf(getStringValue(this.tags.getWaitStrategyTag(), defaultMap, nodeMap)));
        etmConfiguration.setPersistingBulkCount(getIntValue(this.tags.getPersistingBulkCountTag(), defaultMap, nodeMap));
        etmConfiguration.setPersistingBulkSize(getIntValue(this.tags.getPersistingBulkSizeTag(), defaultMap, nodeMap));
        etmConfiguration.setPersistingBulkTime(getIntValue(this.tags.getPersistingBulkTimeTag(), defaultMap, nodeMap));
        etmConfiguration.setPersistingBulkThreads(getIntValue(this.tags.getPersistingBulkThreadsTag(), defaultMap, nodeMap));
        etmConfiguration.setShardsPerIndex(getIntValue(this.tags.getShardsPerIndexTag(), defaultMap, nodeMap));
        etmConfiguration.setReplicasPerIndex(getIntValue(this.tags.getReplicasPerIndexTag(), defaultMap, nodeMap));
        etmConfiguration.setMaxEventIndexCount(getIntValue(this.tags.getMaxEventIndexCountTag(), defaultMap, nodeMap));
        etmConfiguration.setMaxMetricsIndexCount(getIntValue(this.tags.getMaxMetricsIndexCountTag(), defaultMap, nodeMap));
        etmConfiguration.setMaxAuditLogIndexCount(getIntValue(this.tags.getMaxAuditLogIndexCountTag(), defaultMap, nodeMap));
        etmConfiguration.setMaxSearchResultDownloadRows(getIntValue(this.tags.getMaxSearchResultDownloadRowsTag(), defaultMap, nodeMap));
        etmConfiguration.setMaxSearchHistoryCount(getIntValue(this.tags.getMaxSearchHistoryCountTag(), defaultMap, nodeMap));
        etmConfiguration.setMaxSearchTemplateCount(getIntValue(this.tags.getMaxSearchTemplateCountTag(), defaultMap, nodeMap));
        etmConfiguration.setMaxGraphCount(getIntValue(this.tags.getMaxGraphCountTag(), defaultMap, nodeMap));
        etmConfiguration.setMaxDashboardCount(getIntValue(this.tags.getMaxDashboardCountTag(), defaultMap, nodeMap));
        etmConfiguration.setMaxSignalCount(getIntValue(this.tags.getMaxSignalCountTag(), defaultMap, nodeMap));
        etmConfiguration.setWaitForActiveShards(getIntValue(this.tags.getWaitForActiveShardsTag(), defaultMap, nodeMap));
        etmConfiguration.setQueryTimeout(getLongValue(this.tags.getQueryTimeoutTag(), defaultMap, nodeMap));
        etmConfiguration.setRetryOnConflictCount(getIntValue(this.tags.getRetryOnConflictCountTag(), defaultMap, nodeMap));
        if (defaultMap != null) {
            List<Map<String, Object>> remoteClusters = this.converter.getArray(this.tags.getRemoteClustersTag(), defaultMap);
            if (remoteClusters != null) {
                for (var remoteClusterMap : remoteClusters) {
                    var remoteCluster = new RemoteCluster();
                    remoteCluster.setName(this.converter.getString(this.tags.getRemoteClusterNameTag(), remoteClusterMap));
                    remoteCluster.setClusterWide(this.converter.getBoolean(this.tags.getRemoteClusterClusterWideTag(), remoteClusterMap));

                    List<Map<String, Object>> seeds = this.converter.getArray(this.tags.getRemoteClusterSeedsTag(), remoteClusterMap);
                    if (seeds != null) {
                        for (var seedMap : seeds) {
                            var seed = new RemoteCluster.Seed();
                            seed.setHost(this.converter.getString(this.tags.getRemoteClusterSeedHostTag(), seedMap));
                            seed.setPort(this.converter.getInteger(this.tags.getRemoteClusterSeedPortTag(), seedMap));
                            remoteCluster.addSeed(seed);
                        }
                    }
                    etmConfiguration.addRemoteCluster(remoteCluster);
                }
            }
        }
        return etmConfiguration;
    }

    @Override
    public int getActiveNodeCount(String json) {
        var nodeMap = this.converter.toMap(json);
        nodeMap = this.converter.getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE, nodeMap);
        List<Map<String, Object>> instances = this.converter.getArray(this.tags.getInstancesTag(), nodeMap);
        if (instances == null) {
            return 0;
        }
        int count = 0;
        for (var instance : instances) {
            var lastSeen = this.converter.getInstant(this.tags.getLastSeenTag(), instance);
            if (!lastSeen.plus(5, ChronoUnit.MINUTES).isBefore(Instant.now())) {
                count++;
            }
        }
        return count;
    }

    private Integer getIntValue(String tag, Map<String, Object> defaultMap, Map<String, Object> nodeMap) {
        if (nodeMap != null && nodeMap.containsKey(tag)) {
            return ((Number) nodeMap.get(tag)).intValue();
        } else if (defaultMap != null && defaultMap.containsKey(tag)) {
            return ((Number) defaultMap.get(tag)).intValue();
        }
        return null;
    }

    private Long getLongValue(String tag, Map<String, Object> defaultMap, Map<String, Object> nodeMap) {
        if (nodeMap != null && nodeMap.containsKey(tag)) {
            return ((Number) nodeMap.get(tag)).longValue();
        } else if (defaultMap != null && defaultMap.containsKey(tag)) {
            return ((Number) defaultMap.get(tag)).longValue();
        }
        return null;
    }

    private String getStringValue(String tag, Map<String, Object> defaultMap, Map<String, Object> nodeMap) {
        if (nodeMap != null && nodeMap.containsKey(tag)) {
            return (nodeMap.get(tag)).toString();
        } else if (defaultMap != null && defaultMap.containsKey(tag)) {
            return (defaultMap.get(tag)).toString();
        }
        return null;
    }

    private void addIntegerWhenNotDefault(String tag, int defaultValue, int specificValue, JsonBuilder builder) {
        if (defaultValue == specificValue) {
            return;
        }
        builder.field(tag, specificValue);
    }

    private void addLongWhenNotDefault(String tag, long defaultValue, long specificValue, JsonBuilder builder) {
        if (defaultValue == specificValue) {
            return;
        }
        builder.field(tag, specificValue);
    }

    private void addStringWhenNotDefault(String tag, String defaultValue, String specificValue, JsonBuilder builder) {
        if (Objects.equals(defaultValue, specificValue)) {
            return;
        }
        builder.field(tag, specificValue);
    }

    @Override
    public EtmConfigurationTags getTags() {
        return this.tags;
    }

}
