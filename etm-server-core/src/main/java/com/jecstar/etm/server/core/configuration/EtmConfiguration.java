package com.jecstar.etm.server.core.configuration;

import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.util.ObjectUtils;

public class EtmConfiguration {
	//License configuration
	public static final String CONFIG_KEY_LICENSE 							= "license";
	
	// Cluster configuration
	public static final String CONFIG_KEY_SHARDS_PER_INDEX 					= "shardsPerIndex";
	public static final String CONFIG_KEY_REPLICAS_PER_INDEX 				= "replicasPerIndex";
	public static final String CONFIG_KEY_MAX_EVENT_INDEX_COUNT 			= "maxEventIndexCount";
	public static final String CONFIG_KEY_MAX_METRICS_INDEX_COUNT 			= "maxMetricsIndexCount";
	public static final String CONFIG_KEY_WAIT_FOR_ACTIVE_SHARDS 			= "waitForActiveShards";
	public static final String CONFIG_KEY_QUERY_TIMEOUT 					= "queryTimeout";
	public static final String CONFIG_KEY_RETRY_ON_CONFLICT_COUNT 			= "retryOnConflictCount";
	public static final String CONFIG_KEY_MAX_SEARCH_RESULT_DOWNLOAD_ROWS 	= "maxSearchResultDownloadRows";
	public static final String CONFIG_KEY_MAX_SEARCH_TEMPLATE_COUNT 		= "maxSearchTemplateCount";
	public static final String CONFIG_KEY_MAX_SEARCH_HISTORY_COUNT 			= "maxSearchHistoryCount";

	// Node configurations
	public static final String CONFIG_KEY_ENHANCING_HANDLER_COUNT 			= "enhancingHandlerCount";
	public static final String CONFIG_KEY_PERSISTING_HANDLER_COUNT 			= "persistingHandlerCount";
	public static final String CONFIG_KEY_EVENT_BUFFER_SIZE 				= "eventBufferSize";
	public static final String CONFIG_KEY_PERSISTING_BULK_COUNT 			= "persistingBulkCount";
	public static final String CONFIG_KEY_PERSISTING_BULK_SIZE 				= "persistingBulkSize";
	public static final String CONFIG_KEY_PERSISTING_BULK_TIME 				= "persistingBulkTime";
	

	// Disruptor configuration properties.
	private int enhancingHandlerCount = 5;
	private int persistingHandlerCount = 5;
	private int eventBufferSize = 4096;
	
	// Persisting configuration properties;
	private int persistingBulkSize = 1024 * 1024 * 5;
	private int persistingBulkCount = 1000;
	private int persistingBulkTime = 5000;
	
	
	private int shardsPerIndex = 5;
	private int replicasPerIndex = 0;
	private int waitForActiveShards = 1;
	private int retryOnConflictCount = 3;
	
	// Data configuration properties;
	private int maxEventIndexCount = 7; 
	private int maxMetricsIndexCount = 7; 

	// Query options
	private long queryTimeout = 60 * 1000;
	private int maxSearchResultDownloadRows = 500;
	private int maxSearchTemplateCount = 10;
	private int maxSearchHistoryCount = 10;
	
	
	// Other stuff.		
	private final String nodeName;

	private License license;

	private List<ConfigurationChangeListener> changeListeners = new ArrayList<ConfigurationChangeListener>();

	public EtmConfiguration(String nodeName) {
		this.nodeName = nodeName;
	}

	// Etm license configuration

	public License getLicense() {
		return this.license;
	}

	public void setLicenseKey(String licenseKey) {
		if (licenseKey == null || licenseKey.trim().length() == 0) {
			throw new EtmException(EtmException.INVALID_LICENSE_KEY_EXCEPTION);
		}		
		this.license = new License(licenseKey);
	}
	
	// Etm processor configuration

	public int getEnhancingHandlerCount() {
		return this.enhancingHandlerCount;
	}
	
	public EtmConfiguration setEnhancingHandlerCount(int enhancingHandlerCount) {
		if (enhancingHandlerCount >= 0) {
			this.enhancingHandlerCount = enhancingHandlerCount;
		}
		return this;
	}

	public int getPersistingHandlerCount() {
		return this.persistingHandlerCount;
	}
	
	public EtmConfiguration setPersistingHandlerCount(int persistingHandlerCount) {
		if (persistingHandlerCount >=0) {
			this.persistingHandlerCount = persistingHandlerCount;
		}
		return this;
	}

	public int getEventBufferSize() {
		return this.eventBufferSize;
	}
	
	public EtmConfiguration setEventBufferSize(int eventBufferSize) {
		if (eventBufferSize > 0) {
			this.eventBufferSize = eventBufferSize;
		}
		return this;
	}

	// Etm persisting configuration.
	public int getPersistingBulkSize() {
		return this.persistingBulkSize;
	}
	
	public EtmConfiguration setPersistingBulkSize(int persistingBulkSize) {
		if (persistingBulkSize >= 0) {
			this.persistingBulkSize = persistingBulkSize;
		}
		return this;
	}
	
	public int getPersistingBulkCount() {
		return this.persistingBulkCount;
	}
	
	public EtmConfiguration setPersistingBulkCount(int persistingBulkCount) {
		if (persistingBulkCount >= 0) {
			this.persistingBulkCount = persistingBulkCount;
		}
		return this;
	}
	
	public int getPersistingBulkTime() {
		return this.persistingBulkTime;
	}
	
	public EtmConfiguration setPersistingBulkTime(int persistingBulkTime) {
		if (persistingBulkTime >= 0) {
			this.persistingBulkTime = persistingBulkTime;
		}
		return this;
	}
	
	public int getShardsPerIndex() {
		return this.shardsPerIndex;
	}
	
	public EtmConfiguration setShardsPerIndex(int shardsPerIndex) {
		if (shardsPerIndex > 0) {
			this.shardsPerIndex = shardsPerIndex;
		} 
		return this;
	}

	public int getReplicasPerIndex() {
		return this.replicasPerIndex;
	}
	
	public EtmConfiguration setReplicasPerIndex(int replicasPerIndex) {
		if (replicasPerIndex >= 0) {
			this.replicasPerIndex = replicasPerIndex;
		}
		return this;
	}
	
	public int getMaxEventIndexCount() {
		return this.maxEventIndexCount;
	}
	
	public EtmConfiguration setMaxEventIndexCount(int maxEventIndexCount) {
		if (maxEventIndexCount > 0) {
			this.maxEventIndexCount = maxEventIndexCount;
		}
		return this;
	}

	public int getMaxMetricsIndexCount() {
		return this.maxMetricsIndexCount;
	}
	
	public EtmConfiguration setMaxMetricsIndexCount(int maxMetricsIndexCount) {
		if (maxMetricsIndexCount > 0) {
			this.maxMetricsIndexCount = maxMetricsIndexCount;
		}
		return this;
	}
	
	public int getWaitForActiveShards() {
		return this.waitForActiveShards;
	}
	
	public EtmConfiguration setWaitForActiveShards(int waitForActiveShards) {
		if (waitForActiveShards >= -1) {
			this.waitForActiveShards = waitForActiveShards;
		}
		return this;
	}
	
	public int getRetryOnConflictCount() {
		return this.retryOnConflictCount;
	}
	
	public EtmConfiguration setRetryOnConflictCount(int retryOnConflictCount) {
		if (retryOnConflictCount >= 0) {
			this.retryOnConflictCount = retryOnConflictCount;
		}
		return this;
	}
	
	public long getQueryTimeout() {
		return this.queryTimeout;
	}
	
	public EtmConfiguration setQueryTimeout(long queryTimeout) {
		if (queryTimeout > 0) {
			this.queryTimeout = queryTimeout;
		}
		return this;
	}

	public int getMaxSearchResultDownloadRows() {
		return this.maxSearchResultDownloadRows;
	}
	
	public EtmConfiguration setMaxSearchResultDownloadRows(int maxSearchResultDownloadRows) {
		if (maxSearchResultDownloadRows > 0) {
			this.maxSearchResultDownloadRows = maxSearchResultDownloadRows;
		}
		return this;
	}
	
	public int getMaxSearchHistoryCount() {
		return this.maxSearchHistoryCount;
	}
	
	public EtmConfiguration setMaxSearchHistoryCount(int maxSearchHistoryCount) {
		if (maxSearchHistoryCount >=  0) {
			this.maxSearchHistoryCount = maxSearchHistoryCount;
		}
		return this;
	}
	
	public int getMaxSearchTemplateCount() {
		return this.maxSearchTemplateCount;
	}
	
	public EtmConfiguration setMaxSearchTemplateCount(int maxSearchTemplateCount) {
		if (maxSearchTemplateCount >= 0) {
			this.maxSearchTemplateCount = maxSearchTemplateCount;
		}
		return this;
	}

	public String getNodeName() {
		return this.nodeName;
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
		if (this.license == null) {
			return true;
		}
		return !isLicenseValidAt(Instant.now());
	}
	

	public Boolean isLicenseAlmostExpired() {
		if (this.license == null) {
			return false;
		}		
		return !isLicenseExpired() && !isLicenseValidAt(Instant.now().plus(Period.ofDays(14)));
	}
	
	private boolean isLicenseValidAt(Instant moment) {
		if (this.license == null) {
			return false;
		}
		return !moment.isAfter(this.license.getExpiryDate());
	}

	/**
	 * Merge the configuration items from the given
	 * <code>EtmConfiguration</code> into this instance.
	 * <code>ConfigurationChangeListener</code>s are notified in a single event.
	 * 
	 * @param etmConfiguration
	 *            The <code>EtmConfiguration</code> to merge into this instance.
	 * @return <code>true</code> when the configuration is changed,
	 *         <code>false</code> otherwise.
	 */
	public synchronized boolean mergeAndNotify(EtmConfiguration etmConfiguration) {
		if (etmConfiguration == null) {
			return false;
		}
		
		List<String> changed = new ArrayList<String>();
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
		if (this.waitForActiveShards != etmConfiguration.getWaitForActiveShards()) {
			setWaitForActiveShards(etmConfiguration.getWaitForActiveShards());
			changed.add(CONFIG_KEY_WAIT_FOR_ACTIVE_SHARDS);
		}
		if (this.queryTimeout != etmConfiguration.getQueryTimeout()) {
			setQueryTimeout(etmConfiguration.getQueryTimeout());
			changed.add(CONFIG_KEY_QUERY_TIMEOUT);
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
		if (changed.size() > 0) {
			ConfigurationChangedEvent event = new ConfigurationChangedEvent(changed);
			this.changeListeners.forEach(c -> c.configurationChanged(event));
		}
		return changed.size() > 0;
	}
}
