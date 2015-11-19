package com.jecstar.etm.gui.jee.configurator;

import java.net.InetAddress;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.GuiConfiguration;

@ManagedBean
@Singleton
public class ElasticClientProducer {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ElasticClientProducer.class);

	private Client elasticClient;

	private Node node;

	@Produces
	@GuiConfiguration
	public Client getElasticClient() {
		synchronized (this) {
			if (this.elasticClient == null) {
				try {
					String clusterName = System.getProperty("etm.cluster.name");
					if (clusterName == null) {
						clusterName = "Enterprise Telemetry Monitor";
					}
					Builder settings = Settings.settingsBuilder().put("client.transport.sniff", true)
							.put("cluster.name", clusterName)
							.put("path.home", "./");
					String masterAddresses = System.getProperty("etm.master.addresses");
					if (masterAddresses != null) {
						settings.put("node.master", false);
						settings.put("discovery.zen.ping.multicast.enabled", false);
						settings.put("discovery.zen.ping.unicast.hosts", masterAddresses);
					} 
					String nodeName = System.getProperty("etm.node.name");
					if (nodeName != null) {
						nodeName = "GuiNode@" + nodeName;
					} else {
						nodeName = "GuiNode@" + getHostName();
					}
					settings.put("http.enabled", false);
					settings.put("node.name", nodeName);
					this.node = NodeBuilder.nodeBuilder().settings(settings)
						        .client(true)
						        .data(false)
						        .clusterName(clusterName).node();
					this.elasticClient =  this.node.client();
                } catch (Exception e) {
                	this.elasticClient = null;
                	if (log.isErrorLevelEnabled()) {
                		log.logErrorMessage("Error creating elastic client.", e);
                	}
                }
			}
		}
		return this.elasticClient;
	}
	
	private String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			return "local";
		}
	}
	
	@PreDestroy
	public void preDestroy() {
		if (this.elasticClient != null) {
			this.elasticClient.close();
			this.elasticClient = null;
		}
		if (this.node != null) {
			this.node.close();
			this.node = null;
		}
	}
}
