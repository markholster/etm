package com.jecstar.etm.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.wildfly.swarm.container.Container;
import org.wildfly.swarm.jaxrs.JAXRSArchive;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.launcher.converter.ConfigurationConverter;
import com.jecstar.etm.launcher.converter.yaml.ConfigurationConverterYamlImpl;
import com.jecstar.etm.processor.elastic.ElasticBackedEtmConfiguration;
import com.jecstar.etm.processor.elastic.PersistenceEnvironmentElasticImpl;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;
import com.jecstar.etm.processor.rest.RestTelemetryEventProcessor;

public class Startup {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(Startup.class);
	
	private static ConfigurationConverter<Map<String,Object>> configurationConverter = new ConfigurationConverterYamlImpl();
	
	private static TelemetryCommandProcessor processor;
	private static Node node;
	
	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (processor != null) {
					processor.stopAll();
				}
				if (node != null) {
					node.close();
				}
				super.run();
			}
		});
		System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
		try {
			Configuration configuration = loadConfiguration();
			System.setProperty("jboss.bind.address", configuration.bindingAddress);
			System.setProperty("jboss.http.port", "" + configuration.httpPort);
			System.setProperty("jboss.socket.binding.port-offset", "" + configuration.bindingPortOffset);
			Container container = new Container();
			createProcessor(configuration);
			container.start();
			if (configuration.restEnabled) {
	 			JAXRSArchive restProcessor = ShrinkWrap.create(JAXRSArchive.class, "etm-processor-rest.war");
		        restProcessor.addClass(RestTelemetryEventProcessor.class);
		        restProcessor.addAllDependencies();
		        container.deploy(restProcessor);
			}
		} catch (FileNotFoundException e) {
			log.logFatalMessage("Error reading configuration file", e);
		} catch (YamlException e) {
			log.logFatalMessage("Error parsing configuration file", e);
		} catch (Exception e) {
			log.logFatalMessage("Error launching Enterprise Telemetry Monitor", e);
		} 
	}


	private static void createProcessor(Configuration configuration) {
		if (processor == null) {
			if (node == null) {
				node = new NodeBuilder().settings(ImmutableSettings.settingsBuilder()
						.put("cluster.name", configuration.clusterName)
						.put("node.name", configuration.nodeName)
						.put("client.transport.sniff", true)
						.put("http.enabled", false))
//						.put("path.conf", "src/test/resources/config"))
						.client(false)
						.data(true)
						.clusterName(configuration.clusterName)
						.node();
			}
			Client elasticClient = node.client();
			ExecutorService executor = Executors.newCachedThreadPool();
			processor = new TelemetryCommandProcessor();
			EtmConfiguration etmConfiguration = new ElasticBackedEtmConfiguration(configuration.nodeName, "processor", elasticClient);
			processor.start(executor, new PersistenceEnvironmentElasticImpl(etmConfiguration, elasticClient), etmConfiguration);
		}
	}


	@SuppressWarnings("unchecked")
	private static Configuration loadConfiguration() throws FileNotFoundException, YamlException {
		File configDir = new File("conf");
		if (!configDir.exists()) {
			Configuration configuration = new Configuration();
			return configuration;
		} 
		File configFile = new File(configDir, "etm.yaml");
		if (!configFile.exists() || !configFile.isFile() || !configFile.canRead()) {
			Configuration configuration = new Configuration();
			return configuration;				
		}
		YamlReader reader = new YamlReader(new FileReader(configFile));
		Map<String, Object> valueMap = (Map<String, Object>)reader.read();
		return configurationConverter.convert(valueMap);
	}
}
