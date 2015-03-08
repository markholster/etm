package com.jecstar.etm.core.configuration;

import java.io.Closeable;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;

//TODO document this class and the different properties. 
//TODO fallback to default enum values for proprties with illegal values. 
//TODO all properties should be dynamically adjustable. change listeners should be placed on all properties, and processors etc should be restarted accordingly.
public class EtmConfiguration extends AbstractConfiguration implements Closeable {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(EtmConfiguration.class);
	
	private static final String PUBLIC_KEY = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAtBCraSZNjqfqnDK/ESEqwZWZiDY6YRe72N8id//B5LHv7eO41cgRrKzAIn+WH10C3jOjGpJjF1RITKTJg1FM4CK+L66hYP3HQVX8ghtQT99TkHuTkTGxbbBMZd4VF77TR5mTa0LjMTGz7+r9q0PAQEGPol/WqaOTxGHiizh7/qmA0hvAA4Ff39T0CsFyWFpI4hmfS5JG/sLsG8WKd125A1VJFk76ZH7kWP1ysrzGzbR1vSQznQpzz7GPpbzFgjDWJpvQzLREv7qSn1z7MGD4YKlLpgaYxoPUsF2kg4N3YzvZw+RfMTFS2v689VmLccZbySXSoqXyssSq6oMlXIwDSus5qFaB1TeYFJWHZh/t6QHHYeyI0RW6pzIAAG/yGF9uX13uiIb9J9+Qu02XAPstl0ZsVfAVdbzV1AKFMPVOCzMHk6T8YcLsFKedigeH4K2vzdQyHC4L0oZ+2xYiDp904Y7A20HfTyBhVJmz7OIKLjJbnuCh8wP1g9VAR9NC468/nhEdCBxT2nHvvJMLzw2xUBYNIoSw5rWd5+nO9kiCWD7OoNpL5nTRRlX3jBpuqEJmszQo3wF0jZEqAi/pYn3c60iEljtx8m8K8EgjylS/C49qBDUfCQnwfNQxGjxzEzeFc9+mJRox87kxYMUsCyT5u46f8P1wfHOWxzubRgcr0hECAwEAAQ==";
	
	private static final String LIVE_NODES_PATH = "/live_nodes";
	private static final String LICENSE_KEY_PATH = "/license.key";
	
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
	public static final String ETM_DATA_RETENTION_PRESERVE_EVENT_SLAS = "etm.data_retention_preserve_transaction_slas";
	
	private static final String[] CONFIGURATION_KEYS = new String[] { ETM_ENHANCING_HANDLER_COUNT, ETM_INDEXING_HANDLER_COUNT,
	        ETM_PERSISTING_HANDLER_COUNT, ETM_RINGBUFFER_SIZE, ETM_ENDPOINT_CACHE_EXPIRY_TIME, ETM_STATISTICS_TIMEUNIT,
	        ETM_DATA_CORRELATION_MAX_MATCHES, ETM_DATA_CORRELATION_TIME_OFFSET, ETM_DATA_RETENTION_TIME, ETM_DATA_RETENTION_CHECK_INTERVAL, 
	        ETM_DATA_RETENTION_PRESERVE_EVENT_COUNTS, ETM_DATA_RETENTION_PRESERVE_EVENT_PERFORMANCES, ETM_DATA_RETENTION_PRESERVE_EVENT_SLAS};
	
	private CassandraConfiguration cassandraConfiguration;
	private SolrConfiguration solrConfiguration;
	
	private Properties etmProperties;
	
	private CuratorFramework client;

	private NodeCache globalEtmPropertiesNode;
	private NodeCache nodeEtmPropertiesNode;
	private NodeCache licenseNode;

	private String companyName;
	private Date licenseExpiry;
	private LicenseType licenseType;
	
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
		// Load the license
		this.licenseNode = new NodeCache(this.client, LICENSE_KEY_PATH);
		this.licenseNode.getListenable().addListener(new ReloadLicenseListener());
		this.licenseNode.start();
		try {
			loadLicenseData(this.licenseNode);
		} catch (Exception e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Error loading license data", e);
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

	private void loadLicenseData(NodeCache licenseNode) {
	    ChildData currentData = licenseNode.getCurrentData();
	    if (currentData == null) {
	    	this.companyName = "Unknown";
	    	this.licenseExpiry = new Date();
	    	return;
	    }
	    String[] licenseData = decodeLicenseData(currentData.getData());
	    this.companyName = licenseData[0];
	    this.licenseExpiry = new Date(Long.valueOf(licenseData[1]));
	    this.licenseType = LicenseType.valueOf(licenseData[2]);
    }
	
	private String[] decodeLicenseData(byte[] data) {
	    try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
					Base64.getDecoder().decode(PUBLIC_KEY));
			PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
			
		    Cipher decrpyptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		    decrpyptCipher.init(Cipher.DECRYPT_MODE, publicKey);
		    
		    byte[] decryptedBytes = decrpyptCipher.doFinal(Base64.getDecoder().decode(data));
		    String license = new String(decryptedBytes);
		    String[] split = license.split(":");
		    if (split.length != 3) {
		    	throw new EtmException(EtmException.INVALID_LICENSE_KEY_EXCEPTION);
		    }
		    return split;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException
		        | IllegalBlockSizeException | BadPaddingException e) {
			throw new EtmException(EtmException.INVALID_LICENSE_KEY_EXCEPTION, e);
		}
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
		checkDefaultValue(properties, ETM_DATA_RETENTION_PRESERVE_EVENT_COUNTS, "false");
		checkDefaultValue(properties, ETM_DATA_RETENTION_PRESERVE_EVENT_PERFORMANCES, "false");
		checkDefaultValue(properties, ETM_DATA_RETENTION_PRESERVE_EVENT_SLAS, "false");		
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
	
	// Etm license configuration
	
	public String getCompanyName() {
		return this.companyName;
	}
	
	public Date getLicenseExpriy() {
		return this.licenseExpiry;
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
		return Boolean.valueOf(this.etmProperties.getProperty(ETM_DATA_RETENTION_PRESERVE_EVENT_PERFORMANCES));
	}

	public boolean isDataRetentionPreserveEventSlas() {
		return Boolean.valueOf(this.etmProperties.getProperty(ETM_DATA_RETENTION_PRESERVE_EVENT_SLAS));
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
	
	public void setLicenseKey(String licenseKey) {
		// Check if the key is valid, by decoding the String.
		decodeLicenseData(licenseKey.getBytes());
		// TODO Controleren of de huidige versie een trial is en de nieuwe ook. Als dat zo is, dan de key afwijzen?
		try {
			Stat stat = this.client.checkExists().forPath(LICENSE_KEY_PATH);
			if (stat != null) {
				this.client.setData().forPath(LICENSE_KEY_PATH, licenseKey.getBytes());
			} else {
				this.client.create().forPath(LICENSE_KEY_PATH, licenseKey.getBytes());
			}
		} catch (Exception e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
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
	
	private class ReloadLicenseListener implements NodeCacheListener {

		@Override
        public void nodeChanged() throws Exception {
			EtmConfiguration.this.loadLicenseData(EtmConfiguration.this.licenseNode);
        }
		
	}
	
	private enum LicenseType {
		TRIAL, SUBSCRIPTION
	}
 }
