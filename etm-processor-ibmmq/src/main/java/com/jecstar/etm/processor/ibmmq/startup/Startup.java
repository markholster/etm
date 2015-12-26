package com.jecstar.etm.processor.ibmmq.startup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.processor.ibmmq.ElasticBackedEtmConfiguration;
import com.jecstar.etm.processor.ibmmq.PersistenceEnvironmentElasticImpl;
import com.jecstar.etm.processor.ibmmq.configuration.Configuration;
import com.jecstar.etm.processor.ibmmq.configuration.Destination;
import com.jecstar.etm.processor.ibmmq.configuration.QueueManager;
import com.jecstar.etm.processor.processor.PersistenceEnvironment;

public class Startup {
	
	private static Node node;
	private static Client elasticClient;
	private static EtmConfiguration etmConfiguration;
	private static PersistenceEnvironment persistenceEnvironment;
	private static AutoManagedTelemetryEventProcessor processor;

	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (processor != null) {
					try { processor.stop(); } catch (Throwable t) {}
				}
				if (persistenceEnvironment != null) {
					try { persistenceEnvironment.close(); } catch (Throwable t) {}
				}
 				if (elasticClient != null) {
 					try { elasticClient.close(); } catch (Throwable t) {}
				}
 				if (node != null) {
 					try { node.close(); } catch (Throwable t) {}
 				}
			}
		});
		Startup startup = new Startup();
		Configuration configuration = startup.loadConfiguration();
		if (configuration == null) {
			return;
		}
		int nrOfListeners = configuration.getTotalNumberOfListeners();
		
		
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
		
		
	}
	
	private Configuration loadConfiguration() {
		File configFile = new File("config", "etm.yml");
		try (FileReader reader = new FileReader(configFile)) {
			YamlReader ymlReader = new YamlReader(reader);
			ymlReader.getConfig().setClassTag("queuemanager", QueueManager.class);
			ymlReader.getConfig().setClassTag("destination", Destination.class);
			return (Configuration) ymlReader.read();
		} catch (FileNotFoundException e) {
			return new Configuration();
		} catch (IOException e) {
			//TODO Error handling
			e.printStackTrace();
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
	    	//TODO Stuff
	    }
		return null;
	}
}
