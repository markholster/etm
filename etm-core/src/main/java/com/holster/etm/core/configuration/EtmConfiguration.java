package com.holster.etm.core.configuration;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.retry.ExponentialBackoffRetry;

import com.holster.etm.core.EtmException;
import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;

//TODO document this class and the different properties. 
//TODO fallback to default enum values for proprties with illegal values. 
//TODO all properties should be dynamically adjustable. change listeners should be placed on all properties, and processors etc should be restarted accordingly.
public class EtmConfiguration extends AbstractConfiguration implements Closeable {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(EtmConfiguration.class);
	
	public static final String ETM_ENHANCING_HANDLER_COUNT = "etm.enhancing_handler_count";
	public static final String ETM_INDEXING_HANDLER_COUNT = "etm.indexing_handler_count";
	public static final String ETM_PERSISTING_HANDLER_COUNT = "etm.persisting_handler_count";
	public static final String ETM_RINGBUFFER_SIZE = "etm.ringbuffer_size";
	public static final String ETM_ENDPOINT_CACHE_EXPIRY_TIME = "etm.endpoint_cache_expiry_time";
	public static final String ETM_STATISTICS_TIMEUNIT = "etm.statistics_timeunit";
	public static final String ETM_DATA_CORRELATION_MAX_MATCHES = "etm.data_correlation_max_matches";
	public static final String ETM_DATA_CORRELATION_TIME_OFFSET = "etm.data_correlation_time_offset";
	public static final String ETM_DATA_RETENTION_TIME = "etm.data_retention_time";
	public static final String ETM_DATA_RETENTION_CHECK_INTERVAL = "etm.data_retention_check_interval";
	public static final String ETM_DATA_RETENTION_PRESERVE_EVENT_COUNTS = "etm.data_retention_preserve_event_counts";
	public static final String ETM_DATA_RETENTION_PRESERVE_EVENT_PERFORMANCES = "etm.data_retention_preserve_event_performances";
	
	private CassandraConfiguration cassandraConfiguration;
	private SolrConfiguration solrConfiguration;
	
	private Properties etmProperties;
	
	private CuratorFramework client;

	private NodeCache globalEtmPropertiesNode;
	private NodeCache nodeEtmPropertiesNode;

	public synchronized void load(String nodeName, String zkConnections, String namespace) throws Exception {
		if (this.client != null) {
			return;
		}
		String solrZkConnectionString = Arrays.stream(zkConnections.split(",")).map(c -> c + "/" + namespace + "/solr").collect(Collectors.joining(","));
		this.client = CuratorFrameworkFactory.builder().connectString(zkConnections).namespace(namespace).retryPolicy(new ExponentialBackoffRetry(1000, 3)).build();
		this.client.start();
		boolean connected = this.client.blockUntilConnected(30, TimeUnit.SECONDS);
		if (!connected) {
			throw new EtmException(EtmException.CONFIGURATION_LOAD_EXCEPTION);
		}
		
		ReloadEtmPropertiesListener reloadListener = new ReloadEtmPropertiesListener();
		this.globalEtmPropertiesNode = new NodeCache(this.client, "/config/etm.propeties");
		this.globalEtmPropertiesNode.getListenable().addListener(reloadListener);
		this.globalEtmPropertiesNode.start();
		if (nodeName != null) {
			this.nodeEtmPropertiesNode = new NodeCache(this.client, "/config/" + nodeName + "/etm.propeties");
			this.nodeEtmPropertiesNode.getListenable().addListener(reloadListener);
			this.nodeEtmPropertiesNode.start();
		}
		this.etmProperties = loadEtmProperties();
		this.cassandraConfiguration = new CassandraConfiguration(this.client);
		this.solrConfiguration = new SolrConfiguration(client, solrZkConnectionString);
	}

	private Properties loadEtmProperties() {
		Properties properties = new Properties();
		properties.putAll(loadProperties(this.globalEtmPropertiesNode));
		properties.putAll(loadProperties(this.nodeEtmPropertiesNode));
		checkDefaultValue(properties, ETM_ENHANCING_HANDLER_COUNT, "5");
		checkDefaultValue(properties, ETM_INDEXING_HANDLER_COUNT, "5");
		checkDefaultValue(properties, ETM_PERSISTING_HANDLER_COUNT, "5");
		checkDefaultValue(properties, ETM_RINGBUFFER_SIZE, "4096");
		checkDefaultValue(properties, ETM_ENDPOINT_CACHE_EXPIRY_TIME, "60000");
		checkDefaultValue(properties, ETM_STATISTICS_TIMEUNIT, "MINUTES");
		checkDefaultValue(properties, ETM_DATA_CORRELATION_MAX_MATCHES, "100");
		checkDefaultValue(properties, ETM_DATA_CORRELATION_TIME_OFFSET, "30000");
		checkDefaultValue(properties, ETM_DATA_RETENTION_TIME, "604800000");
		checkDefaultValue(properties, ETM_DATA_RETENTION_CHECK_INTERVAL, "60000");
		checkDefaultValue(properties, ETM_DATA_RETENTION_PRESERVE_EVENT_COUNTS, "false");
		checkDefaultValue(properties, ETM_DATA_RETENTION_PRESERVE_EVENT_PERFORMANCES, "false");
		return properties; 
    }
	
	// Cassandra configuration.

	public List<String> getCassandraContactPoints() {
		return this.cassandraConfiguration.getCassandraContactPoints();
	}
	
	public String getCassandraUsername() {
		return this.cassandraConfiguration.getCassandraUsername();
	}
	
	public String getCassandraPassword() {
		return this.cassandraConfiguration.getCassandraUsername();
	}
	
	public String getCassandraKeyspace() {
		return this.cassandraConfiguration.getCassandraKeyspace();
	}
	
	// Solr configuration
	
	public String getSolrZkConnectionString() {
		return this.solrConfiguration.getSolrZkConnectionString();
	}
	
	public String getSolrCollectionName() {
		return this.solrConfiguration.getSolrCollectionName();
	}
	
	// Etm processor configuration
	
	public int getEnhancingHandlerCount() {
		return Integer.valueOf(this.etmProperties.getProperty(ETM_ENHANCING_HANDLER_COUNT));
	}
	
	public int getIndexingHandlerCount() {
		return Integer.valueOf(this.etmProperties.getProperty(ETM_INDEXING_HANDLER_COUNT));
	}
	
	public int getPersistingHandlerCount() {
		return Integer.valueOf(this.etmProperties.getProperty(ETM_PERSISTING_HANDLER_COUNT));
	}
	
	public int getRingbufferSize() {
		return Integer.valueOf(this.etmProperties.getProperty(ETM_RINGBUFFER_SIZE));
	}
	
	// Etm endpoint configuration cache.
	
	public long getEndpointCacheExpiryTime() {
		return Long.valueOf(this.etmProperties.getProperty(ETM_ENDPOINT_CACHE_EXPIRY_TIME));
	}
	
	// Etm statistic configuration
	
	public TimeUnit getStatisticsTimeUnit() {
		return TimeUnit.valueOf(this.etmProperties.getProperty(ETM_STATISTICS_TIMEUNIT));
	}
	
	// Etm data correlation configuration.
	
	public int getDataCorrelationMax() {
		return Integer.valueOf(this.etmProperties.getProperty(ETM_DATA_CORRELATION_MAX_MATCHES));
	}
	
	public long getDataCorrelationTimeOffset() {
		return Long.valueOf(this.etmProperties.getProperty(ETM_DATA_CORRELATION_TIME_OFFSET));
	}
	
	// Etm data retention configuration
	
	public long getDataRetentionTime() {
		return Long.valueOf(this.etmProperties.getProperty(ETM_DATA_RETENTION_TIME));
	}
	
	public long getDataRetentionCheckInterval() {
		return Long.valueOf(this.etmProperties.getProperty(ETM_DATA_RETENTION_CHECK_INTERVAL));
	}

	public boolean isDataRetentionPreserveEventCounts() {
		return Boolean.valueOf(this.etmProperties.getProperty(ETM_DATA_RETENTION_PRESERVE_EVENT_COUNTS));
	}
	
	public boolean isDataRetentionPreserveEventPerformances() {
		return Boolean.valueOf(this.etmProperties.getProperty(ETM_DATA_RETENTION_PRESERVE_EVENT_PERFORMANCES, "false"));
	}
	
	public LeaderSelector createLeaderSelector(String leaderPath, LeaderSelectorListener leaderSelectionListener) {
		return new LeaderSelector(this.client, "/leader-election" + leaderPath, leaderSelectionListener);
    }
	
	@Override
	public void close() {
		if (this.globalEtmPropertiesNode != null) {
			try {
	            this.globalEtmPropertiesNode.close();
            } catch (IOException e) {
            	if (log.isWarningLevelEnabled()) {
            		log.logWarningMessage("Could not close node cache.", e);
            	}
            }
		}
		if (this.nodeEtmPropertiesNode != null) {
			try {
	            this.nodeEtmPropertiesNode.close();
            } catch (IOException e) {
            	if (log.isWarningLevelEnabled()) {
            		log.logWarningMessage("Could not close node cache.", e);
            	}
            }
		}
		if (this.cassandraConfiguration != null) {
			this.cassandraConfiguration.close();
		}
		if (this.solrConfiguration != null) {
			this.solrConfiguration.close();
		}
	    if (this.client != null) {
	    	this.client.close();
	    }
	    this.globalEtmPropertiesNode = null;
	    this.nodeEtmPropertiesNode = null;
	    this.client = null;
	}
	
	private class ReloadEtmPropertiesListener implements NodeCacheListener {

		@Override
        public void nodeChanged() {
			EtmConfiguration.this.etmProperties = EtmConfiguration.this.loadEtmProperties();
        }
		
	}
 }
