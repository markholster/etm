package com.holster.etm.processor.jee.configurator;

import java.util.Properties;

import javax.annotation.ManagedBean;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrServer;

import com.holster.etm.jee.configurator.core.ProcessorConfiguration;

@ManagedBean
@Singleton
public class SolrServerProducer {

	@ProcessorConfiguration
	@Inject
	private Properties configuration;
	
	private SolrServer solrServer;
	
	@Produces
	@ProcessorConfiguration
	public SolrServer getSolrServer() {
		synchronized (this) {
			if (this.solrServer == null) {
				String zookeeperHost = this.configuration.getProperty("solr.zookeeper_host", "127.0.0.1:9983");
	            this.solrServer = new CloudSolrServer(zookeeperHost);
			}
		}
		return this.solrServer;
	}
}
