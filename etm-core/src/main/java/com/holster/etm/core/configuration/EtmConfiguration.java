package com.holster.etm.core.configuration;

import java.io.ByteArrayInputStream;
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
import org.apache.curator.retry.ExponentialBackoffRetry;

import com.holster.etm.core.EtmException;
import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;

public class EtmConfiguration implements Closeable {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(EtmConfiguration.class);
	
	private Properties etmProperties;
	private Properties cassandraProperties;
	private Properties solrProperties;
	
	private CuratorFramework client;

	private String solrZkConnectionString;

	private NodeCache globalPropertiesNodeCache;
	private NodeCache nodePropertiesNodeCache;

	public synchronized void load(String nodeName, String zkConnections, String namespace) throws Exception {
		if (this.client != null) {
			return;
		}
		this.solrZkConnectionString = Arrays.stream(zkConnections.split(",")).map(c -> c + "/" + namespace + "/solr").collect(Collectors.joining(","));
		this.client = CuratorFrameworkFactory.builder().connectString(zkConnections).namespace(namespace).retryPolicy(new ExponentialBackoffRetry(1000, 3)).build();
		this.client.start();
		boolean connected = this.client.blockUntilConnected(30, TimeUnit.SECONDS);
		if (!connected) {
			throw new EtmException(EtmException.CONFIGURATION_LOAD_EXCEPTION);
		}
		
		ReloadEtmPropertiesListener reloadListener = new ReloadEtmPropertiesListener();
		this.globalPropertiesNodeCache = new NodeCache(this.client, "/config/etm.propeties");
		this.globalPropertiesNodeCache.getListenable().addListener(reloadListener);
		this.globalPropertiesNodeCache.start();
		if (nodeName != null) {
			this.nodePropertiesNodeCache = new NodeCache(this.client, "/config/" + nodeName + "/etm.propeties");
			this.nodePropertiesNodeCache.getListenable().addListener(reloadListener);
			this.nodePropertiesNodeCache.start();
		}
		loadEtmProperties();
		loadCassandraProperties();
		loadSolrProperties();
	}

	private synchronized void loadEtmProperties() {
		this.etmProperties = new Properties();
		if (this.globalPropertiesNodeCache.getCurrentData() != null) {
			this.etmProperties.putAll(loadPropertiesFromData(this.globalPropertiesNodeCache.getCurrentData().getData()));
		}
		if (this.nodePropertiesNodeCache != null && this.nodePropertiesNodeCache.getCurrentData() != null) {
			this.etmProperties.putAll(loadPropertiesFromData(this.nodePropertiesNodeCache.getCurrentData().getData()));
		}
    }

	private void loadCassandraProperties() {
	    this.cassandraProperties = loadProperties("/config/cassandra.propeties");
    }
	
	private void loadSolrProperties() {
	    this.solrProperties = loadProperties("/config/solr.propeties");
    }

	private Properties loadProperties(String path) {
		Properties properties = new Properties();
		try {
			if (null == this.client.checkExists().forPath(path)) {
				return properties;
			}
			properties = loadPropertiesFromData(this.client.getData().forPath(path));
        } catch (Exception e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Could not load configuration from path '" + path +"'", e);
        	}
        }
		return properties;
    }
	
	private Properties loadPropertiesFromData(byte[] data) {
		Properties properties = new Properties();
		ByteArrayInputStream bis = null;
		try {
        	bis = new ByteArrayInputStream(data);
        	properties.load(bis);
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Could not load configuration", e);
        	}
        } finally {
        	if (bis != null) {
        		try {
	                bis.close();
                } catch (IOException e) {
                	if (log.isDebugLevelEnabled()) {
                		log.logDebugMessage("Could not close inputstream to configuration", e);
                	}
                }
        	}
        }
		return properties;
	}


	public List<String> getCassandraContactPoints() {
		return Arrays.stream(this.cassandraProperties.getProperty("cassandra.contact_points", "127.0.0.1").split(",")).collect(Collectors.toList());
	}
	
	public String getCassandraUsername() {
		return this.cassandraProperties.getProperty("cassandra.username");
	}
	
	public String getCassandraPassword() {
		return this.cassandraProperties.getProperty("cassandra.password");
	}
	
	public String getCassandraKeyspace() {
		return this.cassandraProperties.getProperty("cassandra.keyspace", "etm");
	}
	
	public String getSolrZkConnectionString() {
		return this.solrZkConnectionString;
	}
	
	public String getSolrCollectionName() {
		return this.solrProperties.getProperty("solr.collection", "etm");
	}
	
	public int getEnhancingHandlerCount() {
		return Integer.valueOf(this.etmProperties.getProperty("etm.enhancing_handler_count", "5"));
	}
	
	public int getIndexingHandlerCount() {
		return Integer.valueOf(this.etmProperties.getProperty("etm.indexing_handler_count", "5"));
	}
	
	public int getPersistingHandlerCount() {
		return Integer.valueOf(this.etmProperties.getProperty("etm.persisting_handler_count", "5"));
	}
	
	public int getRingbufferSize() {
		return Integer.valueOf(this.etmProperties.getProperty("etm.ringbuffer_size", "4096"));
	}
	
	public long getEndpointCacheExpiryTime() {
		return Long.valueOf(this.etmProperties.getProperty("etm.endpoint_cache_expiry_time", "60000"));
	}
	
	public TimeUnit getStatisticsTimeUnit() {
		return TimeUnit.valueOf(this.etmProperties.getProperty("etm.statistics_timeunit", "MINUTES"));
	}
	
	public int getDataCorrelationMax() {
		return Integer.valueOf(this.etmProperties.getProperty("etm.data_correlation_max_matches", "100"));
	}
	
	public long getDataCorrelationTimeOffset() {
		return Long.valueOf(this.etmProperties.getProperty("etm.data_correlation_time_offset", "30000"));
	}
	
	@Override
	public void close() {
		if (this.globalPropertiesNodeCache != null) {
			try {
	            this.globalPropertiesNodeCache.close();
            } catch (IOException e) {
            	if (log.isWarningLevelEnabled()) {
            		log.logWarningMessage("Could not close node cache.", e);
            	}
            }
		}
		if (this.nodePropertiesNodeCache != null) {
			try {
	            this.nodePropertiesNodeCache.close();
            } catch (IOException e) {
            	if (log.isWarningLevelEnabled()) {
            		log.logWarningMessage("Could not close node cache.", e);
            	}
            }
		}
	    if (this.client != null) {
	    	this.client.close();
	    }
	    this.globalPropertiesNodeCache = null;
	    this.nodePropertiesNodeCache = null;
	    this.client = null;
	}
	
	private class ReloadEtmPropertiesListener implements NodeCacheListener {

		@Override
        public void nodeChanged() {
			EtmConfiguration.this.loadEtmProperties();
        }
		
	}
 }
