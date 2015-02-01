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

	private Properties cassandraProperties;
	private NodeCache globalCassandraPropertiesNode;
	
	CassandraConfiguration(CuratorFramework client) throws Exception {
		ReloadCassandraPropertiesListener reloadListener = new ReloadCassandraPropertiesListener();
		this.globalCassandraPropertiesNode = new NodeCache(client, "/config/cassandra.propeties");
		this.globalCassandraPropertiesNode.getListenable().addListener(reloadListener);
		this.globalCassandraPropertiesNode.start();
		this.cassandraProperties = loadCassandraProperties();
    }
	
	private Properties loadCassandraProperties() {
		Properties properties = loadProperties(this.globalCassandraPropertiesNode);
		checkDefaultValue(properties, CASSANDRA_CONTACT_POINTS, "127.0.0.1");
		checkDefaultValue(properties, CASSANDRA_KEYSPACE, "etm");
		return properties;
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
			ConfigurationChangedEvent changedEvent = new ConfigurationChangedEvent(CassandraConfiguration.this.cassandraProperties, newProperties);
			CassandraConfiguration.this.cassandraProperties =  newProperties;
			getConfigurationChangeListeners().forEach(c -> c.configurationChanged(changedEvent));
        }		
	}
}
