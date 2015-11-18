package com.jecstar.etm.processor.jee.configurator;

import java.net.InetAddress;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.ProcessorConfiguration;

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
	@ProcessorConfiguration
	public Client getElasticClient() {
		if (this.elasticClient == null) {
			try {
				String clusterName = System.getProperty("etm.cluster.name");
				if (clusterName == null) {
					clusterName = "Enterprise Telemetry Monitor";
				}
				Builder settings = Settings.settingsBuilder().put("client.transport.sniff", true)
						.put("cluster.name", clusterName)
						.put("path.home", "./");
				String clusterAddresses = System.getProperty("etm.cluster.addresses");
				if (clusterAddresses != null) {
					String[] addresses = clusterAddresses.split(",");
					TransportClient transportClient = TransportClient.builder().settings(settings).build();
					for (String address : addresses) {
						String[] split = address.split(":");
						 transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(split[0]), Integer.valueOf(split[1])));
					}
					this.elasticClient = transportClient;
				} else {
					String nodeName = System.getProperty("etm.node.name");
					if (nodeName == null) {
						nodeName = "ProcessorNode@" + getHostName();
					}
					settings.put("http.enabled", false);
					settings.put("node.name", nodeName);
					this.node = NodeBuilder.nodeBuilder().settings(settings)
						        .client(true)
						        .data(false)
						        .clusterName(clusterName).node();
					this.elasticClient =  this.node.client();
				}
            } catch (Exception e) {
            	this.elasticClient = null;
            	if (log.isErrorLevelEnabled()) {
            		log.logErrorMessage("Error creating elastic client.", e);
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
