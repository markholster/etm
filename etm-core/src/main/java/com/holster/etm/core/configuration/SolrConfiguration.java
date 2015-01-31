package com.holster.etm.core.configuration;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;

import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;

public class SolrConfiguration extends AbstractConfiguration implements Closeable {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(SolrConfiguration.class);

	public static final String SOLR_COLLECTION = "solr.collection";
	
	private Properties solrProperties;
	private NodeCache globalSolrPropertiesNode;

	private String solrZkConnectionString;
	
	SolrConfiguration(CuratorFramework client, String solrZkConnectionString) throws Exception {
		this.solrZkConnectionString = solrZkConnectionString;
		this.globalSolrPropertiesNode = new NodeCache(client, "/config/solr.propeties");
		this.globalSolrPropertiesNode.start();
		this.solrProperties =loadSolrProperties(); 
    }
	
	private Properties loadSolrProperties() {
		Properties properties = loadProperties(this.globalSolrPropertiesNode);
		checkDefaultValue(properties, SOLR_COLLECTION, "etm");
		return properties;
	}
	

	String getSolrZkConnectionString() {
		return this.solrZkConnectionString;
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
	
}
