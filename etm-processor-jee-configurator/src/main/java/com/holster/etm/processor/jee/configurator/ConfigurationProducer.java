package com.holster.etm.processor.jee.configurator;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.holster.etm.core.configuration.EtmConfiguration;
import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;
import com.holster.etm.jee.configurator.core.ProcessorConfiguration;

@ManagedBean
@Singleton
public class ConfigurationProducer {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ConfigurationProducer.class);

	private EtmConfiguration configuration;

	@Produces
	@ProcessorConfiguration
	public EtmConfiguration getEtmConfiguration() {
		synchronized (this) {
			if (this.configuration == null) {
				this.configuration = new EtmConfiguration();
				try {
					String nodename = System.getProperty("etm.nodename");
					String connections = System.getProperty("etm.zookeeper.clients", "127.0.0.1:2181/etm");
					String namespace = System.getProperty("etm.zookeeper.namespace", "dev");
	                this.configuration.load(nodename, connections, namespace, "processor");
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
	
	@PreDestroy
	public void preDestroy() {
		if (this.configuration != null) {
			this.configuration.close();
		}
	}
}
