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
import org.apache.curator.retry.ExponentialBackoffRetry;

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
	
	private CuratorFramework curatorClient;

	private String solrZkConnectionString;

	public void load() throws InterruptedException {
		if (this.curatorClient != null) {
			return;
		}
		String nodename = System.getProperty("etm.nodename");
		String connections = System.getProperty("etm.zookeeper.clients", "127.0.0.1:2181/etm");
		String namespace = System.getProperty("etm.zookeeper.namespace", "dev");
		String zkConnectionString = Arrays.stream(connections.split(",")).map(c -> c + "/" + namespace).collect(Collectors.joining(","));
		this.solrZkConnectionString = Arrays.stream(connections.split(",")).map(c -> c + "/" + namespace + "/solr").collect(Collectors.joining(","));
		this.curatorClient = CuratorFrameworkFactory.builder().connectString(zkConnectionString).namespace(namespace).retryPolicy(new ExponentialBackoffRetry(1000, 3)).build();
		this.curatorClient.blockUntilConnected();
		loadEtmProperties(nodename);
		loadCassandraProperties();
		loadSolrProperties();
	}

	private void loadEtmProperties(String nodename) {
		this.etmProperties = new Properties();
		this.etmProperties.putAll(loadProperties("/config/etm.propeties"));
		if (nodename != null) {
			this.etmProperties.putAll(loadProperties("/config" + nodename + "/etm.propeties"));
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
		ByteArrayInputStream bis = null;
		try {
	        if (null != this.curatorClient.checkExists().forPath(path)) {
	        	bis = new ByteArrayInputStream(this.curatorClient.getData().forPath(path));
	        	properties.load(bis);
	        }
        } catch (Exception e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Could not load configuration '" + path + "' from zookeeper.", e);
        	}
        } finally {
        	if (bis != null) {
        		try {
	                bis.close();
                } catch (IOException e) {
                	if (log.isDebugLevelEnabled()) {
                		log.logDebugMessage("Could not load inputstream to configuration '" + path + "'.", e);
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
		return Integer.valueOf(this.etmProperties.getProperty("etm.data_correlation_max", "100"));
	}
	
	public long getDataCorrelationTimeOffset() {
		return Long.valueOf(this.etmProperties.getProperty("etm.data_correlation_time_offset", "30000"));
	}
	
	@Override
	public void close() throws IOException {
	    if (this.curatorClient != null) {
	    	this.curatorClient.close();
	    }
	    this.curatorClient = null;
	}
}
