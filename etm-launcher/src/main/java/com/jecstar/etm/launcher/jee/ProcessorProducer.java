package com.jecstar.etm.launcher.jee;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.inject.Produces;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.converter.json.EtmConfigurationConverterTagsJsonImpl;
import com.jecstar.etm.launcher.Configuration;
import com.jecstar.etm.processor.elastic.ElasticBackedEtmConfiguration;
import com.jecstar.etm.processor.elastic.PersistenceEnvironmentElasticImpl;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;

public class ProcessorProducer {

	private static Configuration configuration;
	
	private static TelemetryCommandProcessor processor;
	private static Node node;
	
	@Produces
	public synchronized TelemetryCommandProcessor createTelemetryEventProcessor() {
		if (processor == null) {
			if (configuration == null) {
				throw new IllegalStateException();
			}
			node = new NodeBuilder().settings(ImmutableSettings.settingsBuilder()
					.put("cluster.name", configuration.clusterName)
					.put("node.name", configuration.nodeName)
					.put("http.enabled", false))
//					.put("path.conf", "src/test/resources/config"))
					.data(true)
					.node();
			Client elasticClient = node.client();
			ExecutorService executor = Executors.newCachedThreadPool();
			TelemetryCommandProcessor processor = new TelemetryCommandProcessor();
			EtmConfiguration etmConfiguration = new ElasticBackedEtmConfiguration(configuration.nodeName, "processor", elasticClient, new EtmConfigurationConverterTagsJsonImpl());
			processor.start(executor, new PersistenceEnvironmentElasticImpl(etmConfiguration, elasticClient), etmConfiguration);
		}
		return processor;
	}
	
	public static void setConfiguration(Configuration configuration) {
		ProcessorProducer.configuration = configuration;
	}
}
