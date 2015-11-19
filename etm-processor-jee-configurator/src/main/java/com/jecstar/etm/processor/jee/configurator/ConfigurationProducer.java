package com.jecstar.etm.processor.jee.configurator;

import java.net.InetAddress;

import javax.annotation.ManagedBean;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.client.Client;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.jee.configurator.core.ProcessorConfiguration;
import com.jecstar.etm.processor.jee.configurator.elastic.ElasticBackedEtmConfiguration;

@ManagedBean
@Singleton
public class ConfigurationProducer {

	private EtmConfiguration configuration;
	
	private MetricRegistry metricRegistry;
	
	@ProcessorConfiguration
	@Inject
	private Client elasticClient;


	@Produces
	@ProcessorConfiguration
	public EtmConfiguration getEtmConfiguration() {
		synchronized (this) {
			if (this.configuration == null) {
				String nodeName = System.getProperty("etm.node.name");
				if (nodeName != null) {
					nodeName = "ProcessorNode@" + nodeName;
				} else {
					nodeName = "ProcessorNode@" + getHostName();
				}
                this.configuration = new ElasticBackedEtmConfiguration(nodeName, "processor", this.elasticClient);
			}
		}
		return this.configuration;
	}
	
	private String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			return "local";
		}
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

}
