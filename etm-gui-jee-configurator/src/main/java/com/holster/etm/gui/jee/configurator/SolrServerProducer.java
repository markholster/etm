package com.holster.etm.gui.jee.configurator;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrServer;

import com.holster.etm.core.configuration.ConfigurationChangeListener;
import com.holster.etm.core.configuration.ConfigurationChangedEvent;
import com.holster.etm.core.configuration.EtmConfiguration;
import com.holster.etm.core.configuration.SolrConfiguration;
import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;
import com.holster.etm.jee.configurator.core.GuiConfiguration;

@ManagedBean
@Singleton
public class SolrServerProducer implements ConfigurationChangeListener {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(SolrServerProducer.class);

	
	@GuiConfiguration
	@Inject
	private EtmConfiguration configuration;

	private ReconfigurableSolrServer solrServer;

	@Produces
	@GuiConfiguration
	public SolrServer getSolrServer() {
		synchronized (this) {
			if (this.solrServer == null) {
				String solrCollection = this.configuration.getSolrCollectionName();
				this.configuration.addSolrConfigurationChangeListener(this);
				CloudSolrServer cloudSolrServer = new CloudSolrServer(this.configuration.getSolrZkConnectionString());
				cloudSolrServer.setDefaultCollection(solrCollection);
				this.solrServer = new ReconfigurableSolrServer(cloudSolrServer);

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

	@Override
	public void configurationChanged(ConfigurationChangedEvent event) {
		if (event.isAnyChanged(SolrConfiguration.SOLR_ZOOKEEPER_CONNECTION)) {
			if (this.solrServer != null) {
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Detected a change in the configuration that needs to reconnect to Solr cluster.");
				}
				CloudSolrServer newCloudSolrServer = new CloudSolrServer(this.configuration.getSolrZkConnectionString());
				newCloudSolrServer.setDefaultCollection(this.configuration.getSolrCollectionName());
				this.solrServer.switchToCloudSolrServer(newCloudSolrServer);
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Reconnected to Solr cluster.");
				}
			}
		} else if (event.isChanged(SolrConfiguration.SOLR_COLLECTION)) {
			this.solrServer.setDefaultCollection(this.configuration.getSolrCollectionName());
		}
	}
}
