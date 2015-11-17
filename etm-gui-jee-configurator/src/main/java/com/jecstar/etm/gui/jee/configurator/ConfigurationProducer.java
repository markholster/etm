package com.jecstar.etm.gui.jee.configurator;

import java.net.InetAddress;

import javax.annotation.ManagedBean;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.client.Client;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.gui.jee.configurator.elastic.ElasticBackedEtmConfiguration;
import com.jecstar.etm.jee.configurator.core.GuiConfiguration;

@ManagedBean
@Singleton
public class ConfigurationProducer {

	private EtmConfiguration configuration;
	
	@GuiConfiguration
	@Inject
	private Client elasticClient;

	@Produces
	@GuiConfiguration
	public EtmConfiguration getEtmConfiguration() {
		synchronized (this) {
			if (this.configuration == null) {
				String nodeName = System.getProperty("etm.node.name");
				if (nodeName == null) {
					nodeName = "GuiNode@" + getHostName();
				}
                this.configuration = new ElasticBackedEtmConfiguration(nodeName, "gui", this.elasticClient);
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
