package com.jecstar.etm.launcher;

import static io.undertow.servlet.Servlets.servlet;

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
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;

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
import com.jecstar.etm.processor.rest.RestTelemetryEventProcessorApplication;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;

public class Startup {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(Startup.class);

	private static ConfigurationConverter<Map<String, Object>> configurationConverter = new ConfigurationConverterYamlImpl();

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
			}
		});
		try {
			Configuration configuration = loadConfiguration();
			final PathHandler root = Handlers.path();
			final ServletContainer container = ServletContainer.Factory.newInstance();
			Undertow server = Undertow.builder()
					.addHttpListener(configuration.httpPort + configuration.bindingPortOffset,
							configuration.bindingAddress)
					.setHandler(root).build();
			if (configuration.restEnabled) {
				 initializeProcessor(configuration);
				RestTelemetryEventProcessorApplication processorApplication = new RestTelemetryEventProcessorApplication(
						processor);
				ResteasyDeployment deployment = new ResteasyDeployment();
				deployment.setApplication(processorApplication);
				DeploymentInfo di = undertowRestDeployment(deployment, "/");
				di.setClassLoader(processorApplication.getClass().getClassLoader());
				di.setContextPath("/rest/processor/");
				di.setDeploymentName("Rest event processor - " + di.getContextPath());
				DeploymentManager manager = container.addDeployment(di);
				manager.deploy();
				root.addPrefixPath(di.getContextPath(), manager.start());
			}
			server.start();
		} catch (FileNotFoundException e) {
			log.logFatalMessage("Error reading configuration file", e);
		} catch (YamlException e) {
			log.logFatalMessage("Error parsing configuration file", e);
		} catch (Exception e) {
			log.logFatalMessage("Error launching Enterprise Telemetry Monitor", e);
		} finally {
		}
	}

	private static DeploymentInfo undertowRestDeployment(ResteasyDeployment deployment, String mapping) {
		if (mapping == null) {
			mapping = "/";
		}
		if (!mapping.startsWith("/")) {
			mapping = "/" + mapping;
		}
		if (!mapping.endsWith("/")) {
			mapping += "/";
		}
		mapping = mapping + "*";
		String prefix = null;
		if (!mapping.equals("/*")) {
			prefix = mapping.substring(0, mapping.length() - 2);
		}
		ServletInfo resteasyServlet = servlet("ResteasyServlet", HttpServletDispatcher.class).setAsyncSupported(true)
				.setLoadOnStartup(1).addMapping(mapping);
		if (prefix != null) {
			resteasyServlet.addInitParam("resteasy.servlet.mapping.prefix", prefix);
		}
		return new DeploymentInfo().addServletContextAttribute(ResteasyDeployment.class.getName(), deployment)
				.addServlet(resteasyServlet);
	}

	private static void initializeProcessor(Configuration configuration) {
		if (processor == null) {
			if (node == null) {
				node = new NodeBuilder()
						.settings(ImmutableSettings.settingsBuilder()
								.put("cluster.name", configuration.clusterName)
								.put("node.name", configuration.nodeName)
								.put("path.data", configuration.dataPath)
								.put("client.transport.sniff", true)
								.put("http.enabled", false))
						.client(false).data(true).clusterName(configuration.clusterName).node();
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
