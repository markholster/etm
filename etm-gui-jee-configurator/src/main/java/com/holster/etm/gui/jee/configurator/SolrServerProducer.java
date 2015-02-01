package com.holster.etm.gui.jee.configurator;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrServer;

import com.holster.etm.core.configuration.EtmConfiguration;
import com.holster.etm.jee.configurator.core.GuiConfiguration;

@ManagedBean
@Singleton
public class SolrServerProducer {

	@GuiConfiguration
	@Inject
	private EtmConfiguration etmConfiguration;
	
	private SolrServer solrServer;
	
	@Produces
	@GuiConfiguration
	public SolrServer getSolrServer() {
		synchronized (this) {
			if (this.solrServer == null) {
				String solrCollection = this.etmConfiguration.getSolrCollectionName();
				CloudSolrServer cloudSolrServer = new CloudSolrServer(this.etmConfiguration.getSolrZkConnectionString());
				cloudSolrServer.setDefaultCollection(solrCollection);
	            this.solrServer = cloudSolrServer;
			}
		}
		return this.solrServer;
	}
	
	@PreDestroy
	public void preDestroy() {
		if (this.solrServer != null) {
			this.solrServer.shutdown();
		}
	}
}
