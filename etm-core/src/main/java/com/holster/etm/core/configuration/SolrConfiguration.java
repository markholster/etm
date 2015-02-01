package com.holster.etm.core.configuration;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;

import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;

public class SolrConfiguration extends AbstractConfiguration implements Closeable {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(SolrConfiguration.class);

	public static final String SOLR_ZOOKEEPER_CONNECTION = "solr.zookeeper_connection";
	public static final String SOLR_COLLECTION = "solr.collection";
	
	private Properties solrProperties;
	private NodeCache globalSolrPropertiesNode;

	private String defaultSolrZkConnectionString;
	
	SolrConfiguration(CuratorFramework client, String defaultSolrZkConnectionString) throws Exception {
		this.defaultSolrZkConnectionString = defaultSolrZkConnectionString;
		ReloadSolrPropertiesListener reloadListener = new ReloadSolrPropertiesListener();
		this.globalSolrPropertiesNode = new NodeCache(client, "/config/solr.propeties");
		this.globalSolrPropertiesNode.getListenable().addListener(reloadListener);
		this.globalSolrPropertiesNode.start();
		this.solrProperties = loadSolrProperties(); 
    }
	
	private Properties loadSolrProperties() {
		Properties properties = loadProperties(this.globalSolrPropertiesNode);
		checkDefaultValue(properties, SOLR_COLLECTION, "etm");
		checkDefaultValue(properties, SOLR_ZOOKEEPER_CONNECTION, this.defaultSolrZkConnectionString);
		return properties;
	}
	

	String getSolrZkConnectionString() {
		return this.solrProperties.getProperty(SOLR_ZOOKEEPER_CONNECTION);
	}
	
	String getSolrCollectionName() {
		return this.solrProperties.getProperty(SOLR_COLLECTION);
	}
	
	@Override
    public void close() {
		if (this.globalSolrPropertiesNode != null) {
			try {
	            this.globalSolrPropertiesNode.close();
            } catch (IOException e) {
            	if (log.isWarningLevelEnabled()) {
            		log.logWarningMessage("Could not close Cassandra node cache.", e);
            	}
            }			
		}
		this.globalSolrPropertiesNode = null;
    }
	
	private class ReloadSolrPropertiesListener implements NodeCacheListener {

		@Override
        public void nodeChanged() {
			Properties newProperties = SolrConfiguration.this.loadSolrProperties();
			if (newProperties.equals(SolrConfiguration.this.solrProperties)) {
				return;
			}
			ConfigurationChangedEvent changedEvent = new ConfigurationChangedEvent(SolrConfiguration.this.solrProperties, newProperties);
			SolrConfiguration.this.solrProperties =  newProperties;
			getConfigurationChangeListeners().forEach(c -> c.configurationChanged(changedEvent));
        }		
	}
}
