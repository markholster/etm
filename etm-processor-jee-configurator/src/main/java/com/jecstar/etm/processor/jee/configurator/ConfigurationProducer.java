package com.jecstar.etm.processor.jee.configurator;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.ProcessorConfiguration;

@ManagedBean
@Singleton
public class ConfigurationProducer {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ConfigurationProducer.class);

	private EtmConfiguration configuration;
	
	private MetricRegistry metricRegistry;

	@Produces
	@ProcessorConfiguration
	public EtmConfiguration getEtmConfiguration() {
		synchronized (this) {
			if (this.configuration == null) {
				try {
					String nodename = System.getProperty("etm.nodename");
					String connections = System.getProperty("etm.zookeeper.clients", "127.0.0.1:2181/etm");
					String namespace = System.getProperty("etm.zookeeper.namespace", "dev");
	                this.configuration = new EtmConfiguration(nodename, connections, namespace, "processor");
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

	@Produces
	@ProcessorConfiguration
	public MetricRegistry getMetricRegistry() {
		synchronized (this) {
			if (this.metricRegistry == null) {
				this.metricRegistry = new MetricRegistry();
			}
		}
		return this.metricRegistry;
		
	}
	
	@PreDestroy
	public void preDestroy() {
		if (this.configuration != null) {
			this.configuration.close();
		}
	}
}
