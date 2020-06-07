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

package com.jecstar.etm.server.core.domain.configuration;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.ldap.Directory;
import com.jecstar.etm.server.core.util.ObjectUtils;

import java.time.Instant;
import java.time.Period;
import java.util.*;

public class EtmConfiguration {
    //License configuration
    private static final String CONFIG_KEY_LICENSE = "license";

    // Cluster configuration
    private static final String CONFIG_KEY_SESSION_TIMEOUT = "sessionTimeout";
    private static final String CONFIG_KEY_SECRET_HASH = "secretHash";
    public static final String CONFIG_KEY_SHARDS_PER_INDEX = "shardsPerIndex";
    public static final String CONFIG_KEY_REPLICAS_PER_INDEX = "replicasPerIndex";
    private static final String CONFIG_KEY_MAX_EVENT_INDEX_COUNT = "maxEventIndexCount";
    private static final String CONFIG_KEY_MAX_METRICS_INDEX_COUNT = "maxMetricsIndexCount";
    private static final String CONFIG_KEY_MAX_AUDIT_LOG_INDEX_COUNT = "maxAuditLogIndexCount";
    private static final String CONFIG_KEY_WAIT_FOR_ACTIVE_SHARDS = "waitForActiveShards";
    private static final String CONFIG_KEY_QUERY_TIMEOUT = "queryTimeout";
    private static final String CONFIG_KEY_REMOTE_CLUSTERS = "remoteClusters";
    private static final String CONFIG_KEY_RETRY_ON_CONFLICT_COUNT = "retryOnConflictCount";
    private static final String CONFIG_KEY_MAX_SEARCH_RESULT_DOWNLOAD_ROWS = "maxSearchResultDownloadRows";
    private static final String CONFIG_KEY_MAX_SEARCH_TEMPLATE_COUNT = "maxSearchTemplateCount";
    private static final String CONFIG_KEY_MAX_SEARCH_HISTORY_COUNT = "maxSearchHistoryCount";
    private static final String CONFIG_KEY_MAX_GRAPH_COUNT = "maxGraphCount";
    private static final String CONFIG_KEY_MAX_DASHBOARD_COUNT = "maxDashboardCount";
    private static final String CONFIG_KEY_MAX_SIGNAL_COUNT = "maxSignalCount";

    // Node configurations
    public static final String CONFIG_KEY_ENHANCING_HANDLER_COUNT = "enhancingHandlerCount";
    public static final String CONFIG_KEY_PERSISTING_HANDLER_COUNT = "persistingHandlerCount";
    public static final String CONFIG_KEY_EVENT_BUFFER_SIZE = "eventBufferSize";
    public static final String CONFIG_KEY_PERSISTING_BULK_COUNT = "persistingBulkCount";
    public static final String CONFIG_KEY_PERSISTING_BULK_SIZE = "persistingBulkSize";
    public static final String CONFIG_KEY_PERSISTING_BULK_TIME = "persistingBulkTime";
    public static final String CONFIG_KEY_PERSISTING_BULK_THREADS = "persistingBulkThreads";
    public static final String CONFIG_KEY_WAIT_STRATEGY = "waitStrategy";
    public static final String CONFIG_KEY_IMPORT_PROFILE_CACHE_SIZED = "importProfileCacheSize";
    // The secret used to encrypt and decrypt sensitive data.
    public static String secret;

    // Disruptor configuration properties.
    private int enhancingHandlerCount = 5;
    private int persistingHandlerCount = 5;
    private int eventBufferSize = 4096;
    private WaitStrategy waitStrategy = WaitStrategy.BLOCKING;

    // Persisting configuration properties;
    private int persistingBulkSize = 1024 * 1024 * 5;
    private int persistingBulkCount = 1000;
    private int persistingBulkTime = 5000;
    private int persistingBulkThreads = 2;

    private int shardsPerIndex = 5;
    private int replicasPerIndex = 0;
    private int waitForActiveShards = 1;
    private int retryOnConflictCount = 3;

    // Data configuration properties;
    private int maxEventIndexCount = 7;
    private int maxMetricsIndexCount = 7;
    private int maxAuditLogIndexCount = 7;

    // Query options
    private long queryTimeout = 60 * 1000;
    private int maxSearchResultDownloadRows = 500;
    private int maxSearchTemplateCount = 10;
    private int maxSearchHistoryCount = 10;

    // Visualization options
    private int maxGraphCount = 100;
    private int maxDashboardCount = 10;

    // Signal options
    private int maxSignalCount = 10;

    // General options
    private long sessionTimeout = 30 * 60 * 1000;
    private int importProfileCacheSize = 100;

    // Other stuff.
    private final String nodeName;
    private String secretHash;

    private License license;

    private Directory directory;

    private Set<RemoteCluster> remoteClusters = new HashSet<>();

    private final List<ConfigurationChangeListener> changeListeners = new ArrayList<>();

    private final LicenseRateLimiter licenseRateLimiter;

    public EtmConfiguration(String nodeName) {
        this.nodeName = nodeName;
        this.licenseRateLimiter = new LicenseRateLimiter(this);
    }

    // Etm license configuration
    public License getLicense() {
        return this.license;
    }

    public LicenseRateLimiter getLicenseRateLimiter() {
        return this.licenseRateLimiter;
    }

    public void setLicenseKey(String licenseKey) {
        if (licenseKey == null || licenseKey.trim().length() == 0) {
            throw new EtmException(EtmException.INVALID_LICENSE_KEY);
        }
        this.license = new License(licenseKey);
    }

    public Directory getDirectory() {
        return this.directory;
    }

    public void setDirectory(Directory directory) {
        if (this.directory != null) {
            this.directory.close();
        }
        this.directory = directory;
    }

    /**
     * Gives the configured remote clusters.
     *
     * @return The remote clusters.
     */
    public Set<RemoteCluster> getRemoteClusters() {
        return this.remoteClusters;
    }

    /**
     * Adds a <code>RemoteCluster</code> instance to the <code>EtmConfiguration</code>
     *
     * @param remoteCluster The <code>RemoteCluster</code> to add.
     * @return This instance for chaining.
     */
    public EtmConfiguration addRemoteCluster(RemoteCluster remoteCluster) {
        this.remoteClusters.add(remoteCluster);
        return this;
    }

    /**
     * Removes a <code>RemoteCluster</code> instance from the <code>EtmConfiguration</code>
     *
     * @param remoteCluster The <code>RemoteCluster</code> to remove.
     * @return This instance for chaining.
     */
    public EtmConfiguration removeRemoteCluster(RemoteCluster remoteCluster) {
        this.remoteClusters.remove(remoteCluster);
        return this;
    }

    /**
     * Merge the indices of the remote cluster to the given local indices.
     *
     * @param indices The indices of the local cluster.
     * @return The given indices at the local cluster and the same indices at the remote clusters.
     */
    public String[] mergeRemoteIndices(String... indices) {
        var remoteClusters = getRemoteClusters().toArray(new RemoteCluster[0]);
        if (remoteClusters.length == 0) {
            return indices;
        }
        String[] result = new String[indices.length + (indices.length * remoteClusters.length)];
        for (int i = 0; i < indices.length; i++) {
            final int offset = i * (remoteClusters.length + 1);
            result[offset] = indices[i];
            for (int j = 0; j < remoteClusters.length; j++) {
                result[offset + j + 1] = remoteClusters[j].getName() + ":" + indices[i];
            }
        }
        return result;
    }


    /**
     * Method to determine if a license key is valid. This method does not check
     * if the license is expired!
     *
     * @param licenseKey The key to check.
     * @return <code>true</code> if the license is syntactically correct,
     * <code>false</code> otherwise.
     */
    protected boolean isValidLicenseKey(String licenseKey) {
        if (licenseKey == null || licenseKey.trim().length() == 0) {
            return false;
        }
        try {
            new License(licenseKey);
            return true;
        } catch (EtmException e) {
        }
        return false;
    }

    // Etm processor configuration

    public int getEnhancingHandlerCount() {
        return this.enhancingHandlerCount;
    }

    public EtmConfiguration setEnhancingHandlerCount(Integer enhancingHandlerCount) {
        if (enhancingHandlerCount != null && enhancingHandlerCount >= 0) {
            this.enhancingHandlerCount = enhancingHandlerCount;
        }
        return this;
    }

    public int getPersistingHandlerCount() {
        return this.persistingHandlerCount;
    }

    public EtmConfiguration setPersistingHandlerCount(Integer persistingHandlerCount) {
        if (persistingHandlerCount != null && persistingHandlerCount >= 0) {
            this.persistingHandlerCount = persistingHandlerCount;
        }
        return this;
    }

    public int getEventBufferSize() {
        return this.eventBufferSize;
    }

    public EtmConfiguration setEventBufferSize(Integer eventBufferSize) {
        if (eventBufferSize != null && eventBufferSize > 0) {
            this.eventBufferSize = eventBufferSize;
        }
        return this;
    }

    public WaitStrategy getWaitStrategy() {
        return this.waitStrategy;
    }

    public EtmConfiguration setWaitStrategy(WaitStrategy waitStrategy) {
        if (waitStrategy != null) {
            this.waitStrategy = waitStrategy;
        }
        return this;
    }

    // Etm persisting configuration.
    public int getPersistingBulkSize() {
        return this.persistingBulkSize;
    }

    public EtmConfiguration setPersistingBulkSize(Integer persistingBulkSize) {
        if (persistingBulkSize != null && persistingBulkSize >= 0) {
            this.persistingBulkSize = persistingBulkSize;
        }
        return this;
    }

    public int getPersistingBulkCount() {
        return this.persistingBulkCount;
    }

    public EtmConfiguration setPersistingBulkCount(Integer persistingBulkCount) {
        if (persistingBulkCount != null && persistingBulkCount >= 0) {
            this.persistingBulkCount = persistingBulkCount;
        }
        return this;
    }

    public int getPersistingBulkTime() {
        return this.persistingBulkTime;
    }

    public EtmConfiguration setPersistingBulkTime(Integer persistingBulkTime) {
        if (persistingBulkTime != null && persistingBulkTime >= 0) {
            this.persistingBulkTime = persistingBulkTime;
        }
        return this;
    }

    public int getPersistingBulkThreads() {
        return this.persistingBulkThreads;
    }

    public EtmConfiguration setPersistingBulkThreads(Integer persistingBulkThreads) {
        if (persistingBulkThreads != null && persistingBulkThreads >= 1) {
            this.persistingBulkThreads = persistingBulkThreads;
        }
        return this;
    }

    public int getShardsPerIndex() {
        return this.shardsPerIndex;
    }

    public EtmConfiguration setShardsPerIndex(Integer shardsPerIndex) {
        if (shardsPerIndex != null && shardsPerIndex > 0) {
            this.shardsPerIndex = shardsPerIndex;
        }
        return this;
    }

    public int getReplicasPerIndex() {
        return this.replicasPerIndex;
    }

    public EtmConfiguration setReplicasPerIndex(Integer replicasPerIndex) {
        if (replicasPerIndex != null && replicasPerIndex >= 0) {
            this.replicasPerIndex = replicasPerIndex;
        }
        return this;
    }

    public int getMaxEventIndexCount() {
        return this.maxEventIndexCount;
    }

    public EtmConfiguration setMaxEventIndexCount(Integer maxEventIndexCount) {
        if (maxEventIndexCount != null && maxEventIndexCount > 0) {
            this.maxEventIndexCount = maxEventIndexCount;
        }
        return this;
    }

    public int getMaxMetricsIndexCount() {
        return this.maxMetricsIndexCount;
    }

    public EtmConfiguration setMaxMetricsIndexCount(Integer maxMetricsIndexCount) {
        if (maxMetricsIndexCount != null && maxMetricsIndexCount > 0) {
            this.maxMetricsIndexCount = maxMetricsIndexCount;
        }
        return this;
    }

    public int getMaxAuditLogIndexCount() {
        return this.maxMetricsIndexCount;
    }

    public EtmConfiguration setMaxAuditLogIndexCount(Integer maxAuditLogIndexCount) {
        if (maxAuditLogIndexCount != null && maxAuditLogIndexCount >= 7) {
            this.maxAuditLogIndexCount = maxAuditLogIndexCount;
        }
        return this;
    }


    public int getWaitForActiveShards() {
        return this.waitForActiveShards;
    }

    public EtmConfiguration setWaitForActiveShards(Integer waitForActiveShards) {
        if (waitForActiveShards != null && waitForActiveShards >= -1) {
            this.waitForActiveShards = waitForActiveShards;
        }
        return this;
    }

    public int getRetryOnConflictCount() {
        return this.retryOnConflictCount;
    }

    public EtmConfiguration setRetryOnConflictCount(Integer retryOnConflictCount) {
        if (retryOnConflictCount != null && retryOnConflictCount >= 0) {
            this.retryOnConflictCount = retryOnConflictCount;
        }
        return this;
    }

    public long getQueryTimeout() {
        return this.queryTimeout;
    }

    public EtmConfiguration setQueryTimeout(Long queryTimeout) {
        if (queryTimeout != null && queryTimeout > 0) {
            this.queryTimeout = queryTimeout;
        }
        return this;
    }

    public int getMaxSearchResultDownloadRows() {
        return this.maxSearchResultDownloadRows;
    }

    public EtmConfiguration setMaxSearchResultDownloadRows(Integer maxSearchResultDownloadRows) {
        if (maxSearchResultDownloadRows != null && maxSearchResultDownloadRows > 0) {
            this.maxSearchResultDownloadRows = maxSearchResultDownloadRows;
        }
        return this;
    }

    public int getMaxSearchHistoryCount() {
        return this.maxSearchHistoryCount;
    }

    public EtmConfiguration setMaxSearchHistoryCount(Integer maxSearchHistoryCount) {
        if (maxSearchHistoryCount != null && maxSearchHistoryCount >= 0) {
            this.maxSearchHistoryCount = maxSearchHistoryCount;
        }
        return this;
    }

    public int getMaxSearchTemplateCount() {
        return this.maxSearchTemplateCount;
    }

    public EtmConfiguration setMaxSearchTemplateCount(Integer maxSearchTemplateCount) {
        if (maxSearchTemplateCount != null && maxSearchTemplateCount >= 0) {
            this.maxSearchTemplateCount = maxSearchTemplateCount;
        }
        return this;
    }

    public int getMaxGraphCount() {
        return this.maxGraphCount;
    }

    public EtmConfiguration setMaxGraphCount(Integer maxGraphCount) {
        if (maxGraphCount != null && maxGraphCount >= 0) {
            this.maxGraphCount = maxGraphCount;
        }
        return this;
    }

    public int getMaxDashboardCount() {
        return this.maxDashboardCount;
    }

    public EtmConfiguration setMaxDashboardCount(Integer maxDashboardCount) {
        if (maxDashboardCount != null && maxDashboardCount >= 0) {
            this.maxDashboardCount = maxDashboardCount;
        }
        return this;
    }

    public int getMaxSignalCount() {
        return this.maxSignalCount;
    }

    public EtmConfiguration setMaxSignalCount(Integer maxSignalCount) {
        if (maxSignalCount != null && maxSignalCount >= 0) {
            this.maxSignalCount = maxSignalCount;
        }
        return this;
    }

    public long getSessionTimeout() {
        return this.sessionTimeout;
    }

    public EtmConfiguration setSessionTimeout(Long sessionTimeout) {
        if (sessionTimeout != null && sessionTimeout >= 60000) {
            this.sessionTimeout = sessionTimeout;
        }
        return this;
    }

    public int getImportProfileCacheSize() {
        return this.importProfileCacheSize;
    }

    public EtmConfiguration setImportProfileCacheSize(Integer importProfileCacheSize) {
        if (importProfileCacheSize != null && importProfileCacheSize >= 0) {
            this.importProfileCacheSize = importProfileCacheSize;
        }
        return this;
    }

    public String getNodeName() {
        return this.nodeName;
    }

    public String getSecretHash() {
        return this.secretHash;
    }

    public EtmConfiguration setSecretHash(String secretHash) {
        this.secretHash = secretHash;
        return this;
    }

    public void addConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener) {
        if (!this.changeListeners.contains(configurationChangeListener)) {
            this.changeListeners.add(configurationChangeListener);
        }
    }

    public void removeConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener) {
        this.changeListeners.remove(configurationChangeListener);
    }

    public boolean isLicenseExpired() {
        return !isLicenseExpired(Instant.now());
    }

    public Boolean isLicenseAlmostExpired() {
        return !isLicenseExpired() && !isLicenseExpired(Instant.now().plus(Period.ofDays(14)));
    }

    private boolean isLicenseExpired(Instant moment) {
        return this.license != null && this.license.isExpiredAt(moment);
    }

    public boolean isLicenseValid() {
        return this.license != null && this.license.isValidAt(Instant.now());
    }

    public Boolean isLicenseSizeExceeded() {
        return this.license == null;
    }

    /**
     * Gives the number of active nodes within the cluster.
     *
     * @return The number of active nodes.
     */
    public Integer getActiveNodeCount() {
        return 1;
    }

    /**
     * Merge the configuration items from the given
     * <code>EtmConfiguration</code> into this instance.
     * <code>ConfigurationChangeListener</code>s are notified in a single event.
     *
     * @param etmConfiguration The <code>EtmConfiguration</code> to merge into this instance.
     * @return <code>true</code> when the configuration is changed,
     * <code>false</code> otherwise.
     */
    public synchronized boolean mergeAndNotify(EtmConfiguration etmConfiguration) {
        if (etmConfiguration == null) {
            return false;
        }

        List<String> changed = new ArrayList<>();
        if (!ObjectUtils.equalsNullProof(this.license, etmConfiguration.getLicense())) {
            this.license = etmConfiguration.getLicense();
            changed.add(CONFIG_KEY_LICENSE);
        }
        if (this.enhancingHandlerCount != etmConfiguration.getEnhancingHandlerCount()) {
            setEnhancingHandlerCount(etmConfiguration.getEnhancingHandlerCount());
            changed.add(CONFIG_KEY_ENHANCING_HANDLER_COUNT);
        }
        if (this.persistingHandlerCount != etmConfiguration.getPersistingHandlerCount()) {
            setPersistingHandlerCount(etmConfiguration.getPersistingHandlerCount());
            changed.add(CONFIG_KEY_PERSISTING_HANDLER_COUNT);
        }
        if (this.eventBufferSize != etmConfiguration.getEventBufferSize()) {
            setEventBufferSize(etmConfiguration.getEventBufferSize());
            changed.add(CONFIG_KEY_EVENT_BUFFER_SIZE);
        }
        if (!this.waitStrategy.equals(etmConfiguration.waitStrategy)) {
            setWaitStrategy(etmConfiguration.waitStrategy);
            changed.add(CONFIG_KEY_WAIT_STRATEGY);
        }
        if (this.persistingBulkSize != etmConfiguration.getPersistingBulkSize()) {
            setPersistingBulkSize(etmConfiguration.getPersistingBulkSize());
            changed.add(CONFIG_KEY_PERSISTING_BULK_SIZE);
        }
        if (this.persistingBulkCount != etmConfiguration.getPersistingBulkCount()) {
            setPersistingBulkCount(etmConfiguration.getPersistingBulkCount());
            changed.add(CONFIG_KEY_PERSISTING_BULK_COUNT);
        }
        if (this.persistingBulkTime != etmConfiguration.getPersistingBulkTime()) {
            setPersistingBulkTime(etmConfiguration.getPersistingBulkTime());
            changed.add(CONFIG_KEY_PERSISTING_BULK_TIME);
        }
        if (this.persistingBulkThreads != etmConfiguration.getPersistingBulkThreads()) {
            setPersistingBulkThreads(etmConfiguration.getPersistingBulkThreads());
            changed.add(CONFIG_KEY_PERSISTING_BULK_THREADS);
        }
        if (this.shardsPerIndex != etmConfiguration.getShardsPerIndex()) {
            setShardsPerIndex(etmConfiguration.getShardsPerIndex());
            changed.add(CONFIG_KEY_SHARDS_PER_INDEX);
        }
        if (this.replicasPerIndex != etmConfiguration.getReplicasPerIndex()) {
            setReplicasPerIndex(etmConfiguration.replicasPerIndex);
            changed.add(CONFIG_KEY_REPLICAS_PER_INDEX);
        }
        if (this.maxEventIndexCount != etmConfiguration.getMaxEventIndexCount()) {
            setMaxEventIndexCount(etmConfiguration.getMaxEventIndexCount());
            changed.add(CONFIG_KEY_MAX_EVENT_INDEX_COUNT);
        }
        if (this.maxMetricsIndexCount != etmConfiguration.getMaxMetricsIndexCount()) {
            setMaxMetricsIndexCount(etmConfiguration.getMaxMetricsIndexCount());
            changed.add(CONFIG_KEY_MAX_METRICS_INDEX_COUNT);
        }
        if (this.maxAuditLogIndexCount != etmConfiguration.getMaxAuditLogIndexCount()) {
            setMaxAuditLogIndexCount(etmConfiguration.getMaxAuditLogIndexCount());
            changed.add(CONFIG_KEY_MAX_AUDIT_LOG_INDEX_COUNT);
        }
        if (this.waitForActiveShards != etmConfiguration.getWaitForActiveShards()) {
            setWaitForActiveShards(etmConfiguration.getWaitForActiveShards());
            changed.add(CONFIG_KEY_WAIT_FOR_ACTIVE_SHARDS);
        }
        if (this.queryTimeout != etmConfiguration.getQueryTimeout()) {
            setQueryTimeout(etmConfiguration.getQueryTimeout());
            changed.add(CONFIG_KEY_QUERY_TIMEOUT);
        }
        if (!Objects.equals(this.remoteClusters, etmConfiguration.getRemoteClusters())) {
            this.remoteClusters = etmConfiguration.getRemoteClusters();
            changed.add(CONFIG_KEY_REMOTE_CLUSTERS);
        }

        if (this.retryOnConflictCount != etmConfiguration.getRetryOnConflictCount()) {
            setRetryOnConflictCount(etmConfiguration.getRetryOnConflictCount());
            changed.add(CONFIG_KEY_RETRY_ON_CONFLICT_COUNT);
        }
        if (this.maxSearchResultDownloadRows != etmConfiguration.getMaxSearchResultDownloadRows()) {
            setMaxSearchResultDownloadRows(etmConfiguration.getMaxSearchResultDownloadRows());
            changed.add(CONFIG_KEY_MAX_SEARCH_RESULT_DOWNLOAD_ROWS);
        }
        if (this.maxSearchTemplateCount != etmConfiguration.getMaxSearchTemplateCount()) {
            setMaxSearchTemplateCount(etmConfiguration.getMaxSearchTemplateCount());
            changed.add(CONFIG_KEY_MAX_SEARCH_TEMPLATE_COUNT);
        }
        if (this.maxSearchHistoryCount != etmConfiguration.getMaxSearchHistoryCount()) {
            setMaxSearchHistoryCount(etmConfiguration.getMaxSearchHistoryCount());
            changed.add(CONFIG_KEY_MAX_SEARCH_HISTORY_COUNT);
        }
        if (this.maxGraphCount != etmConfiguration.getMaxGraphCount()) {
            setMaxGraphCount(etmConfiguration.getMaxGraphCount());
            changed.add(CONFIG_KEY_MAX_GRAPH_COUNT);
        }
        if (this.maxDashboardCount != etmConfiguration.getMaxDashboardCount()) {
            setMaxDashboardCount(etmConfiguration.getMaxDashboardCount());
            changed.add(CONFIG_KEY_MAX_DASHBOARD_COUNT);
        }
        if (this.maxSignalCount != etmConfiguration.getMaxSignalCount()) {
            setMaxSignalCount(etmConfiguration.getMaxSignalCount());
            changed.add(CONFIG_KEY_MAX_SIGNAL_COUNT);
        }
        if (this.sessionTimeout != etmConfiguration.getSessionTimeout()) {
            setSessionTimeout(etmConfiguration.getSessionTimeout());
            changed.add(CONFIG_KEY_SESSION_TIMEOUT);
        }
        if (this.importProfileCacheSize != etmConfiguration.getImportProfileCacheSize()) {
            setImportProfileCacheSize(etmConfiguration.getImportProfileCacheSize());
            changed.add(CONFIG_KEY_IMPORT_PROFILE_CACHE_SIZED);
        }
        if (changed.size() > 0) {
            ConfigurationChangedEvent event = new ConfigurationChangedEvent(changed);
            this.changeListeners.forEach(c -> c.configurationChanged(event));
        }
        if (!Objects.equals(this.secretHash, etmConfiguration.getSecretHash())) {
            setSecretHash(etmConfiguration.getSecretHash());
            changed.add(CONFIG_KEY_SECRET_HASH);
        }
        return changed.size() > 0;
    }
}
