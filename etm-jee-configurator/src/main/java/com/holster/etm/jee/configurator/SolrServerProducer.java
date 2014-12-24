package com.holster.etm.jee.configurator;

import java.net.MalformedURLException;
import java.util.Properties;

import javax.annotation.ManagedBean;
import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.LBHttpSolrServer;

import com.holster.etm.processor.EtmException;

@ManagedBean
public class SolrServerProducer {

	@EtmConfiguration
	@Inject
	private Properties configuration;
	
	private SolrServer solrServer;
	
	public SolrServer getSolrServer() {
		synchronized (this) {
			if (this.solrServer == null) {
				String serverUrls = this.configuration.getProperty("solr.server_urls");
				String[] split = serverUrls.split(",");
				try {
	                this.solrServer = new LBHttpSolrServer(split);
                } catch (MalformedURLException e) {
                	throw new EtmException(EtmException.SOLR_URL_EXCEPTION, e);
                }
			}
		}
		return this.solrServer;
	}
}
