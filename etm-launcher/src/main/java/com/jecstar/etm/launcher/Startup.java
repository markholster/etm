package com.jecstar.etm.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.processor.elastic.ElasticBackedEtmConfiguration;
import com.jecstar.etm.processor.elastic.PersistenceEnvironmentElasticImpl;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;

public class Startup {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(Startup.class);

	private static TelemetryCommandProcessor processor;
	private static HttpServer httpServer;
	private static Node node;
	private static Client elasticClient;

	public static void main(String[] args) {
		addShutdownHook();
		try {
			final Configuration configuration = loadConfiguration();
			if (configuration.isProcessorNecessary()) {
				initializeProcessor(configuration);
			}
			if (configuration.isHttpServerNecessary()) {
				 httpServer = new HttpServer(configuration, processor);
				 httpServer.start();
			}
			if (log.isInfoLevelEnabled()) {
				log.logInfoMessage("Enterprise Telemetry Monitor started.");
			}
		} catch (FileNotFoundException e) {
			if (log.isFatalLevelEnabled()) {
				log.logFatalMessage("Error reading configuration file", e);
			}
		} catch (YamlException e) {
			if (log.isFatalLevelEnabled()) {
				log.logFatalMessage("Error parsing configuration file", e);
			}
		} catch (Exception e) {
			if (log.isFatalLevelEnabled()) {
				log.logFatalMessage("Error launching Enterprise Telemetry Monitor", e);
			}
		} finally {
		}
	}

	private static void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (httpServer != null) {
					try { httpServer.stop(); } catch (Throwable t) {}
				}
				if (processor != null) {
					try { processor.stopAll(); } catch (Throwable t) {}
				}
				if (elasticClient != null) {
					try { elasticClient.close(); } catch (Throwable t) {}
				}
				if (node != null) {
					try { node.close(); } catch (Throwable t) {}
				}
			}
		});
	}

	private static void initializeProcessor(Configuration configuration) {
		if (processor == null) {
			initializeElasticsearchClient(configuration);
			ExecutorService executor = Executors.newCachedThreadPool();
			processor = new TelemetryCommandProcessor();
			EtmConfiguration etmConfiguration = new ElasticBackedEtmConfiguration(configuration.instanceName, "processor", elasticClient);
			processor.start(executor, new PersistenceEnvironmentElasticImpl(etmConfiguration, elasticClient), etmConfiguration);
		}
	}
	
	private static void initializeElasticsearchClient(Configuration configuration) {
		if (elasticClient != null) {
			return;
		}
		if (configuration.elasticsearch.connectAsNode) {
			if (node == null) {
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
				node = new NodeBuilder()
						.settings(settingsBuilder)
						.client(!configuration.elasticsearch.nodeData)
						.data(configuration.elasticsearch.nodeData)
						.clusterName(configuration.clusterName)
						.node();
			}
			elasticClient = node.client();
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
			elasticClient = transportClient;
		}
	}

	private static Configuration loadConfiguration() throws FileNotFoundException, YamlException {
		File configDir = new File("config");
		if (!configDir.exists()) {
			Configuration configuration = new Configuration();
			return configuration;
		}
		File configFile = new File(configDir, "etm.yml");
		if (!configFile.exists() || !configFile.isFile() || !configFile.canRead()) {
			Configuration configuration = new Configuration();
			return configuration;
		}
		YamlReader reader = new YamlReader(new FileReader(configFile));
		return reader.read(Configuration.class);
	}
}
