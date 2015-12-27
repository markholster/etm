package com.jecstar.etm.processor.ibmmq.startup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.ibm.mq.MQEnvironment;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.ibmmq.DestinationReader;
import com.jecstar.etm.processor.ibmmq.ElasticBackedEtmConfiguration;
import com.jecstar.etm.processor.ibmmq.PersistenceEnvironmentElasticImpl;
import com.jecstar.etm.processor.ibmmq.configuration.Configuration;
import com.jecstar.etm.processor.ibmmq.configuration.Destination;
import com.jecstar.etm.processor.processor.PersistenceEnvironment;

public class Startup {
	
	private static final LogWrapper log = LogFactory.getLogger(Startup.class);
	
	private static Node node;
	private static Client elasticClient;
	private static EtmConfiguration etmConfiguration;
	private static PersistenceEnvironment persistenceEnvironment;
	private static AutoManagedTelemetryEventProcessor processor;

	private static ExecutorService executorService;

	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (executorService != null) {
					if (log.isInfoLevelEnabled()) {
						log.logInfoMessage("Shutting down listeners...");
					}
					executorService.shutdown();
					try {
						executorService.awaitTermination(1, TimeUnit.MINUTES);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				if (processor != null) {
					if (log.isInfoLevelEnabled()) {
						log.logInfoMessage("Shutting down event processor...");
					}
					try { processor.stop(); } catch (Throwable t) {}
				}
				if (persistenceEnvironment != null) {
					if (log.isInfoLevelEnabled()) {
						log.logInfoMessage("Closing persistence environment...");
					}
					try { persistenceEnvironment.close(); } catch (Throwable t) {}
				}
 				if (elasticClient != null) {
					if (log.isInfoLevelEnabled()) {
						log.logInfoMessage("Closing cluster client...");
					}
 					try { elasticClient.close(); } catch (Throwable t) {}
				}
 				if (node != null) {
					if (log.isInfoLevelEnabled()) {
						log.logInfoMessage("Closing cluster node...");
					}
 					try { node.close(); } catch (Throwable t) {}
 				}
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Shutdown complete.");
				}
			}
		});
		Startup startup = new Startup();
		Configuration configuration = startup.loadConfiguration();
		if (configuration == null) {
			return;
		}
		int nrOfListeners = configuration.getTotalNumberOfListeners();
		if (nrOfListeners < 1) {
			if (log.isInfoLevelEnabled()) {
				log.logInfoMessage("No listeners configured, nothing to do.");
			}
			return;
		}
		node = startup.createElasticNode(configuration);
		if (node == null) {
			return;
		}
		elasticClient = node.client();
		etmConfiguration = new ElasticBackedEtmConfiguration(configuration.getCalculatedNodeName(), "processor", elasticClient);
		persistenceEnvironment = new PersistenceEnvironmentElasticImpl(etmConfiguration, elasticClient);
		persistenceEnvironment.createEnvironment();
		processor = new AutoManagedTelemetryEventProcessor(etmConfiguration, persistenceEnvironment, elasticClient);
		processor.start();
		
		MQEnvironment.hostname = configuration.getQueueManager().getHost();
		MQEnvironment.port = configuration.getQueueManager().getPort();
		MQEnvironment.channel = configuration.getQueueManager().getChannel();
		
		executorService = Executors.newFixedThreadPool(nrOfListeners);
		System.out.println(new Date());
		for (Destination destination : configuration.getQueueManager().getDestinations()) {
			for (int i=0; i < destination.getNrOfListeners(); i++) {
				executorService.submit(new DestinationReader(processor, configuration.getQueueManager(), destination));
			}
		}
	}
	
	private Configuration loadConfiguration() {
		File configFile = new File("config", "etm.yml");
		try (FileReader reader = new FileReader(configFile)) {
			YamlReader ymlReader = new YamlReader(reader);
			ymlReader.getConfig().setClassTag("configuration", Configuration.class);
			ymlReader.getConfig().setClassTag("destination", Destination.class);
			return (Configuration) ymlReader.read();
		} catch (FileNotFoundException e) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("No config file at '" + configFile.getPath() + "'. Using defaults instead.");
			}
			return new Configuration();
		} catch (IOException e) {
			e.printStackTrace();
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Failed to load configuration", e);
			}
		}		
		return null;
	}
	
	private Node createElasticNode(Configuration configuration) {
		try {
			Builder settings = Settings.settingsBuilder().put("client.transport.sniff", true)
					.put("cluster.name", configuration.getClusterName())
					.put("path.home", "./");
			String masterAddresses = configuration.getMasterAddresses();
			if (masterAddresses != null) {
				settings.put("node.master", false);
				settings.put("discovery.zen.ping.multicast.enabled", false);
				settings.put("discovery.zen.ping.unicast.hosts", masterAddresses);
			} 
			settings.put("http.enabled", false);
			settings.put("node.name", configuration.getCalculatedNodeName());
			return NodeBuilder.nodeBuilder().settings(settings)
				        .client(true)
				        .data(false)
				        .clusterName(configuration.getClusterName()).node();
	    } catch (Exception e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Failed to connect to cluster", e);
			}
	    }
		return null;
	}
}
