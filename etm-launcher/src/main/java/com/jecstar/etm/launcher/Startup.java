package com.jecstar.etm.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.wildfly.swarm.container.Container;
import org.wildfly.swarm.jaxrs.JAXRSArchive;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.launcher.converter.ConfigurationConverter;
import com.jecstar.etm.launcher.converter.yaml.ConfigurationConverterYamlImpl;
import com.jecstar.etm.processor.rest.RestTelemetryEventProcessor;

public class Startup {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(Startup.class);
	
	private static ConfigurationConverter<Map<String,Object>> configurationConverter = new ConfigurationConverterYamlImpl();
	
	public static void main(String[] args) {
		try {
			Configuration config = loadConfiguration();
			System.setProperty("jboss.bind.address", config.bindingAddress);
			System.setProperty("jboss.http.port", "" + config.httpPort);
			System.setProperty("jboss.socket.binding.port-offset", "" + config.bindingPortOffset);
			
			Container container = new Container();
			container.start();
			if (config.restEnabled) {
	 			JAXRSArchive deployment = ShrinkWrap.create(JAXRSArchive.class, "etm-processor-rest.war");
		        deployment.addClass(RestTelemetryEventProcessor.class);
		        deployment.addAllDependencies();
		        container.deploy(deployment);
			}
		} catch (FileNotFoundException e) {
			log.logFatalMessage("Error reading configuration file", e);
		} catch (YamlException e) {
			log.logFatalMessage("Error parsing configuration file", e);
		} catch (Exception e) {
			log.logFatalMessage("Error launching Enterprise Telemetry Monitor", e);
		}
	}


	@SuppressWarnings("unchecked")
	private static Configuration loadConfiguration() throws FileNotFoundException, YamlException {
		File configDir = new File("../conf");
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
