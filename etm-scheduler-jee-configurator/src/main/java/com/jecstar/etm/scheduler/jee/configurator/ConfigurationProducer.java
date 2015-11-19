package com.jecstar.etm.scheduler.jee.configurator;

import java.net.InetAddress;

import javax.annotation.ManagedBean;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.client.Client;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.jee.configurator.core.SchedulerConfiguration;
import com.jecstar.etm.scheduler.jee.configurator.elastic.ElasticBackedEtmConfiguration;

@ManagedBean
@Singleton
public class ConfigurationProducer {

	private EtmConfiguration configuration;
	
	@SchedulerConfiguration
	@Inject
	private Client elasticClient;


	@Produces
	@SchedulerConfiguration
	public EtmConfiguration getEtmConfiguration() {
		synchronized (this) {
			if (this.configuration == null) {
				String nodeName = System.getProperty("etm.node.name");
				if (nodeName != null) {
					nodeName = "SchedulerNode@" + nodeName;
				} else {
					nodeName = "SchedulerNode@" + getHostName();
				}
	            this.configuration = new ElasticBackedEtmConfiguration(nodeName, "scheduler", this.elasticClient);
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
}
