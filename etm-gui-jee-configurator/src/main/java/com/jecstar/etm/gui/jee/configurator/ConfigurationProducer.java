package com.jecstar.etm.gui.jee.configurator;

import javax.annotation.ManagedBean;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.client.Client;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.gui.jee.configurator.elastic.ElasticBackedEtmConfiguration;
import com.jecstar.etm.jee.configurator.core.GuiConfiguration;

@ManagedBean
@Singleton
public class ConfigurationProducer {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ConfigurationProducer.class);

	private EtmConfiguration configuration;
	
	@GuiConfiguration
	@Inject
	private Client elasticClient;

	@Produces
	@GuiConfiguration
	public EtmConfiguration getEtmConfiguration() {
		synchronized (this) {
			if (this.configuration == null) {
				try {
					String nodeName = System.getProperty("etm.node.name");
					if (nodeName == null) {
						nodeName = "Node_1@local";
					}
	                this.configuration = new ElasticBackedEtmConfiguration(nodeName, "gui", this.elasticClient);
                } catch (Exception e) {
                	this.configuration = null;
                	if (log.isErrorLevelEnabled()) {
                		log.logErrorMessage("Error loading etm configuration.", e);
                	}
                }
			}
		}
		return this.configuration;
	}
}
