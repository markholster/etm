package com.holster.etm.gui.jee.configurator;

import java.util.Properties;

import javax.annotation.ManagedBean;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrServer;

import com.holster.etm.jee.configurator.core.GuiConfiguration;

@ManagedBean
@Singleton
public class SolrServerProducer {

	@GuiConfiguration
	@Inject
	private Properties configuration;
	
	private SolrServer solrServer;
	
	@Produces
	@GuiConfiguration
	public SolrServer getSolrServer() {
		synchronized (this) {
			if (this.solrServer == null) {
				String zookeeperHost = this.configuration.getProperty("solr.zookeeper_host", "127.0.0.1:9983");
				String solrCollection = this.configuration.getProperty("solr.collection", "etm");
				CloudSolrServer cloudSolrServer = new CloudSolrServer(zookeeperHost);
				cloudSolrServer.setDefaultCollection(solrCollection);
	            this.solrServer = cloudSolrServer;
			}
		}
		return this.solrServer;
	}
}
