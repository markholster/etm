package com.holster.etm.core.configuration;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;

import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;

public class CassandraConfiguration extends AbstractConfiguration implements Closeable {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(CassandraConfiguration.class);

	public static final String CASSANDRA_CONTACT_POINTS = "cassandra.contact_points";
	public static final String CASSANDRA_USERNAME = "cassandra.username";
	public static final String CASSANDRA_PASSWORD = "cassandra.password";
	public static final String CASSANDRA_KEYSPACE = "cassandra.keyspace";

	private static final String[] CONFIGURATION_KEYS = new String[] { CASSANDRA_CONTACT_POINTS, CASSANDRA_USERNAME, CASSANDRA_PASSWORD, CASSANDRA_KEYSPACE };
	
	private final CuratorFramework client;
	private Properties cassandraProperties;
	private NodeCache globalCassandraPropertiesNode;
	private NodeCache nodeCassandraPropertiesNode;

	
	CassandraConfiguration(CuratorFramework client, String nodeName) throws Exception {
		this.client = client;
		ReloadCassandraPropertiesListener reloadListener = new ReloadCassandraPropertiesListener();
		this.globalCassandraPropertiesNode = new NodeCache(this.client, NODE_CONFIGURATION_PATH + "/cassandra.properties");
		this.globalCassandraPropertiesNode.getListenable().addListener(reloadListener);
		this.globalCassandraPropertiesNode.start();
		if (nodeName != null) {
			this.nodeCassandraPropertiesNode = new NodeCache(this.client, NODE_CONFIGURATION_PATH + "/" + nodeName + "/cassandra.properties");
			this.nodeCassandraPropertiesNode.getListenable().addListener(reloadListener);
			this.nodeCassandraPropertiesNode.start();
		}		
		this.cassandraProperties = loadCassandraProperties();
    }
	
	private Properties loadCassandraProperties() {
		Properties properties = new Properties();
		properties.putAll(loadProperties(this.globalCassandraPropertiesNode));
		properties.putAll(loadProperties(this.nodeCassandraPropertiesNode));
		fillDefaults(properties);
		return properties;
	}
	
	private void fillDefaults(Properties properties) {
		checkDefaultValue(properties, CASSANDRA_CONTACT_POINTS, "127.0.0.1");
		checkDefaultValue(properties, CASSANDRA_KEYSPACE, "etm");		
	}
	
	List<String> getCassandraContactPoints() {
		return Arrays.stream(this.cassandraProperties.getProperty(CASSANDRA_CONTACT_POINTS).split(",")).collect(Collectors.toList());
	}
	
	String getCassandraUsername() {
		return this.cassandraProperties.getProperty(CASSANDRA_USERNAME);
	}
	
	String getCassandraPassword() {
		return this.cassandraProperties.getProperty(CASSANDRA_PASSWORD);
	}
	
	String getCassandraKeyspace() {
		return this.cassandraProperties.getProperty(CASSANDRA_KEYSPACE);
	}
	
	Properties getNodeConfiguration(String nodeName) {
		Properties properties = getNodeConfiguration(this.client, nodeName, "cassandra.properties");
		fillDefaults(properties);
		return properties;
	}
	
	public void update(String nodeName, Properties properties) {
		Properties defaultValues = new Properties();
		fillDefaults(defaultValues);
		updateNodeConfiguration(this.client, nodeName, "cassandra.properties", CONFIGURATION_KEYS, defaultValues, properties);
    }
	
	@Override
    public void close() {
		if (this.globalCassandraPropertiesNode != null) {
			try {
	            this.globalCassandraPropertiesNode.close();
            } catch (IOException e) {
            	if (log.isWarningLevelEnabled()) {
            		log.logWarningMessage("Could not close Cassandra node cache.", e);
            	}
            }			
		}
		this.globalCassandraPropertiesNode = null;
    }
	
	private class ReloadCassandraPropertiesListener implements NodeCacheListener {

		@Override
        public void nodeChanged() {
			Properties newProperties = CassandraConfiguration.this.loadCassandraProperties();
			if (newProperties.equals(CassandraConfiguration.this.cassandraProperties)) {
				return;
			}						
			if (log.isInfoLevelEnabled()) {
				log.logInfoMessage("Change in cassandra.properties detected. Broadcasting configuration change event.");
			}
			ConfigurationChangedEvent changedEvent = new ConfigurationChangedEvent(CassandraConfiguration.this.cassandraProperties, newProperties);
			CassandraConfiguration.this.cassandraProperties =  newProperties;
			getConfigurationChangeListeners().forEach(c -> c.configurationChanged(changedEvent));
        }		
	}
}
