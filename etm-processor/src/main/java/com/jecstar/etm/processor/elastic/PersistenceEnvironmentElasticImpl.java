package com.jecstar.etm.processor.elastic;

import java.io.IOException;

import org.elasticsearch.client.Client;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.processor.CommandResources;
import com.jecstar.etm.processor.processor.PersistenceEnvironment;

public class PersistenceEnvironmentElasticImpl implements PersistenceEnvironment {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(PersistenceEnvironmentElasticImpl.class);

	private final EtmConfiguration etmConfiguration;
	private final Client elasticClient;
	private CommandResources commandResources;

	public PersistenceEnvironmentElasticImpl(final EtmConfiguration etmConfiguration, final Client elasticClient) {
		this.etmConfiguration = etmConfiguration;
		this.elasticClient = elasticClient;
	}
	
	@Override
	public CommandResources getCommandResources(final MetricRegistry metricRegistry) {
		synchronized (this) {
			if (this.commandResources == null) {
				this.commandResources = new CommandResourcesElasticImpl(this.elasticClient, this.etmConfiguration, metricRegistry);
			}
		}
		return this.commandResources;
	}
	
	@Override
	public void close() {
		if (this.commandResources != null) {
			try {
				this.commandResources.close();
			} catch (IOException e) {
				if (log.isErrorLevelEnabled()) {
					log.logErrorMessage("Failed to close CommandResources", e);
				}
			}
		}
	}

}
