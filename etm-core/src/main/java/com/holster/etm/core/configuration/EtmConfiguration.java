package com.holster.etm.core.configuration;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

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
	
	private static final String LIVE_NODES_PATH = "/live_nodes";
	
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
	
	private static final String[] CONFIGURATION_KEYS = new String[] { ETM_ENHANCING_HANDLER_COUNT, ETM_INDEXING_HANDLER_COUNT,
	        ETM_PERSISTING_HANDLER_COUNT, ETM_RINGBUFFER_SIZE, ETM_ENDPOINT_CACHE_EXPIRY_TIME, ETM_STATISTICS_TIMEUNIT,
	        ETM_DATA_CORRELATION_MAX_MATCHES, ETM_DATA_CORRELATION_TIME_OFFSET, ETM_DATA_RETENTION_TIME, ETM_DATA_RETENTION_CHECK_INTERVAL};
	
	private CassandraConfiguration cassandraConfiguration;
	private SolrConfiguration solrConfiguration;
	
	private Properties etmProperties;
	
	private CuratorFramework client;

	private NodeCache globalEtmPropertiesNode;
	private NodeCache nodeEtmPropertiesNode;
	
	public EtmConfiguration(String nodeName, String zkConnections, String namespace, String component) throws Exception {
		String solrZkConnectionString = Arrays.stream(zkConnections.split(",")).map(c -> c + "/" + namespace + "/solr").collect(Collectors.joining(","));
		this.client = CuratorFrameworkFactory.builder().connectString(zkConnections).namespace(namespace).retryPolicy(new ExponentialBackoffRetry(1000, 3)).build();
		this.client.start();
		try {
			boolean connected = this.client.blockUntilConnected(30, TimeUnit.SECONDS);
			if (!connected) {
				throw new EtmException(EtmException.CONFIGURATION_LOAD_EXCEPTION);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtmException(EtmException.CONFIGURATION_LOAD_EXCEPTION, e);
		}
		if (nodeName != null) {
			//TODO wrap this in transactions.
			Stat stat = this.client.checkExists().forPath(NODE_CONFIGURATION_PATH + "/" + nodeName);
			if (stat == null) {
				this.client.create().creatingParentsIfNeeded().forPath(NODE_CONFIGURATION_PATH + "/" + nodeName);
			}
			stat = this.client.checkExists().forPath(LIVE_NODES_PATH);
			if (stat == null) {
				this.client.create().creatingParentsIfNeeded().forPath(LIVE_NODES_PATH);
			}
			stat = this.client.checkExists().forPath(LIVE_NODES_PATH + "/" + nodeName);
			if (stat == null) {
				this.client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(LIVE_NODES_PATH +"/" + nodeName);
			}
		}
		ReloadEtmPropertiesListener reloadListener = new ReloadEtmPropertiesListener();
		this.globalEtmPropertiesNode = new NodeCache(this.client, NODE_CONFIGURATION_PATH + "/etm.properties");
		this.globalEtmPropertiesNode.getListenable().addListener(reloadListener);
		this.globalEtmPropertiesNode.start();
		if (nodeName != null) {
			this.nodeEtmPropertiesNode = new NodeCache(this.client, NODE_CONFIGURATION_PATH + "/" + nodeName + "/etm.properties");
			this.nodeEtmPropertiesNode.getListenable().addListener(reloadListener);
			this.nodeEtmPropertiesNode.start();
		}
		this.etmProperties = loadEtmProperties(this.globalEtmPropertiesNode, this.nodeEtmPropertiesNode);
		this.cassandraConfiguration = new CassandraConfiguration(this.client, nodeName);
		this.solrConfiguration = new SolrConfiguration(this.client, solrZkConnectionString);
	}

	private Properties loadEtmProperties(NodeCache globalNodeCache, NodeCache nodeNodeCache) {
		Properties properties = new Properties();
		properties.putAll(loadProperties(globalNodeCache));
		properties.putAll(loadProperties(nodeNodeCache));
		fillDefaults(properties);
		return properties; 
    }
	
	private void fillDefaults(Properties properties) {
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
	
	public LeaderSelector createLeaderSelector(String leaderPath, LeaderSelectorListener leaderSelectionListener) {
		return new LeaderSelector(this.client, "/leader-election" + leaderPath, leaderSelectionListener);
    }
	
	public void addEtmConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener) {
		addConfigurationChangeListener(configurationChangeListener);
	}
	
	public void removeEtmConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener) {
		removeConfigurationChangeListener(configurationChangeListener);
	}
	
	public void addSolrConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener) {
		this.solrConfiguration.addConfigurationChangeListener(configurationChangeListener);
	}
	
	public void removeSolrConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener) {
		this.solrConfiguration.removeConfigurationChangeListener(configurationChangeListener);
	}
	
	public void addCassandraConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener) {
		this.cassandraConfiguration.addConfigurationChangeListener(configurationChangeListener);
	}
	
	public void removeCassandraConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener) {
		this.cassandraConfiguration.removeConfigurationChangeListener(configurationChangeListener);
	}
	
	/**
	 * Returns a list with nodes that have connected to the ETM cluster at least
	 * one time.
	 * 
	 * @return A list with nodes.
	 */
	public List<Node> getNodes() {
		try {
			List<String> nodeNames = this.client.getChildren().forPath(NODE_CONFIGURATION_PATH);
			List<String> liveNodes = getLiveNodes();
			if (nodeNames != null) {
				return nodeNames.stream().filter(c -> !c.endsWith(".properties")).map(c -> new Node(c, liveNodes.contains(c))).collect(Collectors.toList());
			}
		} catch (Exception e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
		return Collections.emptyList();
	}
	
	/**
	 * Gives a list with nodes that are connected to the ETM cluster at this
	 * moment.
	 * 
	 * @return A list with live nodes.
	 */
	public List<String> getLiveNodes() {
		try {
			return this.client.getChildren().forPath(LIVE_NODES_PATH);
		} catch (Exception e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}	
	
	/**
	 * Gives the configuration parameters of a given node.
	 * 
	 * @param nodeName
	 *            The name of the node.
	 * @return The configuration parameters of the node.
	 */
	public Properties getNodeConfiguration(String nodeName) {
		Properties properties = getNodeConfiguration(this.client, nodeName, "etm.properties");
		fillDefaults(properties);
		properties.putAll(this.cassandraConfiguration.getNodeConfiguration(nodeName));
		properties.putAll(this.solrConfiguration.getNodeConfiguration(nodeName));
		return properties;
	}
	
	public void update(String nodeName, Properties properties) {
		Properties defaultValues = new Properties();
		fillDefaults(defaultValues);
		updateNodeConfiguration(this.client, nodeName, "etm.properties", CONFIGURATION_KEYS, defaultValues, properties);
		this.cassandraConfiguration.update(nodeName, properties);
		this.solrConfiguration.update(nodeName, properties);
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
			Properties newProperties = EtmConfiguration.this.loadEtmProperties(EtmConfiguration.this.globalEtmPropertiesNode, EtmConfiguration.this.nodeEtmPropertiesNode);
			if (newProperties.equals(EtmConfiguration.this.etmProperties)) {
				return;
			}
			// TODO Afhandeling wanneer beide properties tegelijk worden aangepast.
			if (log.isInfoLevelEnabled()) {
				log.logInfoMessage("Change in etm.properties detected. Broadcasting configuration change event.");
			}
			ConfigurationChangedEvent changedEvent = new ConfigurationChangedEvent(EtmConfiguration.this.etmProperties, newProperties);
			EtmConfiguration.this.etmProperties =  newProperties;
			getConfigurationChangeListeners().forEach(c -> {
				try {
					c.configurationChanged(changedEvent);
				} catch (Exception e) {
					if (log.isErrorLevelEnabled()) {
						log.logErrorMessage("Error processing change event", e);
					}
				}});
        }		
	}
 }
