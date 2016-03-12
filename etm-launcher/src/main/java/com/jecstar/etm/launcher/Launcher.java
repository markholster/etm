package com.jecstar.etm.launcher;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.launcher.http.ElasticsearchIdentityManager;
import com.jecstar.etm.launcher.http.HttpServer;
import com.jecstar.etm.processor.elastic.ElasticBackedEtmConfiguration;
import com.jecstar.etm.processor.elastic.PersistenceEnvironmentElasticImpl;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;
import com.jecstar.etm.slf4j.InternalEtmLogForwarder;

public class Launcher {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(Launcher.class);

	private TelemetryCommandProcessor processor;
	private HttpServer httpServer;
	private Node node;
	private Client elasticClient;

	
	public void launch(CommandLineParameters commandLineParameters, Configuration configuration) {
		addShutdownHooks();
		try {
			initializeElasticsearchClient(configuration);
			initializeProcessor(configuration);
			InternalEtmLogForwarder.processor = processor;
			if (configuration.isHttpServerNecessary()) {
				System.setProperty("org.jboss.logging.provider", "slf4j");
				this.httpServer = new HttpServer(new ElasticsearchIdentityManager(this.elasticClient), configuration, this.processor, this.elasticClient);
				this.httpServer.start();
			}
			if (!commandLineParameters.isQuiet()) {
				System.out.println("Enterprise Telemetry Monitor started.");
			}
			if (log.isInfoLevelEnabled()) {
				log.logInfoMessage("Enterprise Telemetry Monitor started.");
			}
		} catch (Exception e) {
			if (!commandLineParameters.isQuiet()) {
				e.printStackTrace();
			}
			if (log.isFatalLevelEnabled()) {
				log.logFatalMessage("Error launching Enterprise Telemetry Monitor", e);
			}
		}		
	}
	
	private void addShutdownHooks() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Shutting down Enterprise Telemetry Monitor.");
				}
				if (Launcher.this.httpServer != null) {
					try { Launcher.this.httpServer.stop(); } catch (Throwable t) {}
				}
				if (Launcher.this.processor != null) {
					try { Launcher.this.processor.stopAll(); } catch (Throwable t) {}
				}
				if (Launcher.this.elasticClient != null) {
					try { Launcher.this.elasticClient.close(); } catch (Throwable t) {}
				}
				if (Launcher.this.node != null) {
					try { Launcher.this.node.close(); } catch (Throwable t) {}
				}
			}
		});
	}
	
	private void initializeProcessor(Configuration configuration) {
		if (this.processor == null) {
			this.processor = new TelemetryCommandProcessor();
			EtmConfiguration etmConfiguration = new ElasticBackedEtmConfiguration(configuration.instanceName, "processor", this.elasticClient);
			this.processor.start(Executors.defaultThreadFactory(), new PersistenceEnvironmentElasticImpl(etmConfiguration, this.elasticClient), etmConfiguration);
		}
	}
	
	private void initializeElasticsearchClient(Configuration configuration) {
		if (this.elasticClient != null) {
			return;
		}
		if (configuration.elasticsearch.connectAsNode) {
			if (this.node == null) {
				Builder settingsBuilder = Settings.settingsBuilder()
					.put("cluster.name", configuration.clusterName)
					.put("node.name", configuration.instanceName)
					.put("path.home", configuration.elasticsearch.nodeHomePath)
					.put("path.data", configuration.elasticsearch.nodeDataPath)
					.put("path.logs", configuration.elasticsearch.nodeLogPath)
					.put("http.enabled", false);
				if (configuration.getElasticsearchTransportPort() > 0) {
					settingsBuilder.put("transport.tcp.port", configuration.getElasticsearchTransportPort());
				}
				if (!configuration.elasticsearch.nodeMulticast) {
					settingsBuilder.put("discovery.zen.ping.multicast.enabled", false);
					settingsBuilder.put("discovery.zen.ping.unicast.hosts", configuration.elasticsearch.connectAddresses);
				}
				this.node = new NodeBuilder()
						.settings(settingsBuilder)
						.client(!configuration.elasticsearch.nodeData)
						.data(configuration.elasticsearch.nodeData)
						.clusterName(configuration.clusterName)
						.node();
			}
			this.elasticClient = node.client();
		} else {
			TransportClient transportClient = TransportClient.builder().settings(Settings.settingsBuilder()
					.put("cluster.name", configuration.clusterName)
					.put("client.transport.sniff", true)).build();
			String[] hosts = configuration.elasticsearch.connectAddresses.split(",");
			for (String host : hosts) {
				int ix = host.lastIndexOf(":");
				if (ix != -1) {
					try {
						InetAddress inetAddress = InetAddress.getByName(host.substring(0, ix));
						int port = Integer.parseInt(host.substring(ix + 1));
						transportClient.addTransportAddress(new InetSocketTransportAddress(inetAddress, port));
					} catch (UnknownHostException e) {
						if (log.isWarningLevelEnabled()) {
							log.logWarningMessage("Unable to connect to '" + host + "'", e);
						}
					}
				}
			}
			this.elasticClient = transportClient;
		}
		new ElasticsearchIndextemplateCreator().createTemplates(this.elasticClient);
	}
}
