package com.jecstar.etm.processor.jee.configurator;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.client.Client;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.ProcessorConfiguration;
import com.jecstar.etm.processor.jee.configurator.elastic.PersistenceEnvironmentElasticImpl;
import com.jecstar.etm.processor.processor.PersistenceEnvironment;

@ManagedBean
@Singleton
public class PersistenceEnvironmentProducer {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(PersistenceEnvironmentProducer.class);

	@ProcessorConfiguration
	@Inject
	private EtmConfiguration configuration;

	@ProcessorConfiguration
	@Inject
	private Client elasticClient;
	
	@ProcessorConfiguration
	@Inject
	private MetricRegistry metricRegistry;

	private PersistenceEnvironment persistenceEnvironment;
	
	@ProcessorConfiguration
	@Produces
	public PersistenceEnvironment getPersistenceEnvironment() {
		synchronized (this) {
			if (this.persistenceEnvironment == null) {
				this.persistenceEnvironment = new PersistenceEnvironmentElasticImpl(this.configuration, this.elasticClient);
				this.persistenceEnvironment.createEnvironment();
			}
		}
		return this.persistenceEnvironment;
	}

	@PreDestroy
	public void preDestroy() {
		this.persistenceEnvironment.close();
	}
}
