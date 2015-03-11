package com.jecstar.etm.gui.jee.configurator;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;

import com.jecstar.etm.core.configuration.ConfigurationChangeListener;
import com.jecstar.etm.core.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.configuration.SolrConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.GuiConfiguration;

@ManagedBean
@Singleton
public class SolrClientProducer implements ConfigurationChangeListener {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(SolrClientProducer.class);

	
	@GuiConfiguration
	@Inject
	private EtmConfiguration configuration;

	private ReconfigurableSolrClient solrClient;

	@Produces
	@GuiConfiguration
	public SolrClient getSolrClient() {
		synchronized (this) {
			if (this.solrClient == null) {
				String solrCollection = this.configuration.getSolrCollectionName();
				this.configuration.addSolrConfigurationChangeListener(this);
				CloudSolrClient cloudSolrClient = new CloudSolrClient(this.configuration.getSolrZkConnectionString());
				cloudSolrClient.setDefaultCollection(solrCollection);
				this.solrClient = new ReconfigurableSolrClient(cloudSolrClient);

			}
		}
		return this.solrClient;
	}

	@PreDestroy
	public void preDestroy() {
		if (this.solrClient != null) {
			this.solrClient.shutdown();
		}
	}

	@Override
	public void configurationChanged(ConfigurationChangedEvent event) {
		if (event.isAnyChanged(SolrConfiguration.SOLR_ZOOKEEPER_CONNECTION)) {
			if (this.solrClient != null) {
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Detected a change in the configuration that needs ETM to reconnect to Solr cluster.");
				}
				CloudSolrClient newCloudSolrClient = new CloudSolrClient(this.configuration.getSolrZkConnectionString());
				newCloudSolrClient.setDefaultCollection(this.configuration.getSolrCollectionName());
				this.solrClient.switchToCloudSolrClient(newCloudSolrClient);
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Reconnected to Solr cluster.");
				}
			}
		} else if (event.isChanged(SolrConfiguration.SOLR_COLLECTION)) {
			this.solrClient.setDefaultCollection(this.configuration.getSolrCollectionName());
		}
	}
}
