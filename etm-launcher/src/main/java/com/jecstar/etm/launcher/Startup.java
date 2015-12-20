package com.jecstar.etm.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

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

public class Startup {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(Startup.class);

	private static ConfigurationConverter<Map<String, Object>> configurationConverter = new ConfigurationConverterYamlImpl();

	private static TelemetryCommandProcessor processor;
	private static HttpServer httpServer;
	private static Node node;

	public static void main(String[] args) {
		try {
			final Configuration configuration = loadConfiguration();
			if (configuration.isProcessorNecessary()) {
				initializeProcessor(configuration);
			}
			if (configuration.isHttpServerNecessary()) {
				 httpServer = new HttpServer(configuration, processor);
				 httpServer.start();
			}
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					if (httpServer != null) {
						httpServer.stop();
					}
					if (processor != null) {
						processor.stopAll();
					}
					if (node != null) {
						node.close();
					}
				}
			});
		} catch (FileNotFoundException e) {
			log.logFatalMessage("Error reading configuration file", e);
		} catch (YamlException e) {
			log.logFatalMessage("Error parsing configuration file", e);
		} catch (Exception e) {
			log.logFatalMessage("Error launching Enterprise Telemetry Monitor", e);
		} finally {
		}
	}

	private static void initializeProcessor(Configuration configuration) {
		if (processor == null) {
			if (node == null) {
				node = new NodeBuilder()
						.settings(Settings.settingsBuilder()
								.put("cluster.name", configuration.clusterName)
								.put("node.name", configuration.nodeName)
								.put("path.home", configuration.homePath)
								.put("path.data", configuration.dataPath)
								.put("client.transport.sniff", true)
								.put("http.enabled", false))
						.client(!configuration.nodeData)
						.data(configuration.nodeData)
						.clusterName(configuration.clusterName)
						.node();
			}
			Client elasticClient = node.client();
			ExecutorService executor = Executors.newCachedThreadPool();
			processor = new TelemetryCommandProcessor();
			EtmConfiguration etmConfiguration = new ElasticBackedEtmConfiguration(configuration.nodeName, "processor",
					elasticClient);
			processor.start(executor, new PersistenceEnvironmentElasticImpl(etmConfiguration, elasticClient),
					etmConfiguration);
		}
	}

	@SuppressWarnings("unchecked")
	private static Configuration loadConfiguration() throws FileNotFoundException, YamlException {
		File configDir = new File("config");
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
		Map<String, Object> valueMap = (Map<String, Object>) reader.read();
		return configurationConverter.convert(valueMap);
	}
}
