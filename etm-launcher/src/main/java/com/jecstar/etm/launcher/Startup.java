package com.jecstar.etm.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;

import org.yaml.snakeyaml.Yaml;

import com.jecstar.etm.domain.builders.ApplicationBuilder;
import com.jecstar.etm.domain.builders.EndpointBuilder;
import com.jecstar.etm.domain.builders.EndpointHandlerBuilder;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.launcher.slf4j.EtmLoggerFactory;
import com.jecstar.etm.launcher.slf4j.LogConfiguration;
import com.jecstar.etm.processor.internal.persisting.BusinessEventLogger;
import com.jecstar.etm.processor.internal.persisting.InternalBulkProcessorWrapper;

public class Startup {

	public static void main(String[] args) {
		CommandLineParameters commandLineParameters = new CommandLineParameters(args);
		if (!commandLineParameters.isProceedNormalStartup()) {
			return;
		}
		try {
			Configuration configuration = loadConfiguration(new File(commandLineParameters.getConfigDirectory()));
			LogConfiguration.loggers.putAll(configuration.logging.loggers);
			LogConfiguration.rootLogLevel = configuration.logging.rootLogger;
			LogConfiguration.applicationInstance = configuration.instanceName;
			LogConfiguration.hostAddress = InetAddress.getByName(configuration.bindingAddress);
			InternalBulkProcessorWrapper bulkProcessorWrapper = new InternalBulkProcessorWrapper();
			EtmLoggerFactory.initialize(bulkProcessorWrapper);
			BusinessEventLogger.initialize(bulkProcessorWrapper,
					new EndpointBuilder().setName(configuration.instanceName)
							.setWritingEndpointHandler(new EndpointHandlerBuilder().setHandlingTime(ZonedDateTime.now())
									.setApplication(new ApplicationBuilder().setName("Enterprise Telemetry Monitor")
											.setVersion(System.getProperty("app.version"))
											.setInstance(configuration.instanceName)
											.setPrincipal(System.getProperty("user.name"))
											.setHostAddress(InetAddress.getByName(configuration.bindingAddress)))));
			new Launcher().launch(commandLineParameters, configuration, bulkProcessorWrapper);
		} catch (FileNotFoundException e) {
			System.err.println("Configuration file not found: " + e.getMessage());
			return;
		} catch (UnknownHostException e) {
			System.err.println("Invalid binding address: " + e.getMessage());
			return;
		} catch (IOException e) {
			System.err.println("Error loading configuration: " + e.getMessage());
			return;
		}
	}

	/**
	 * Loads the configuration from the configuration file and overrides the
	 * options provided in the environment.
	 * 
	 * @param configDir
	 *            The directory that contains the "etm.yml" configuration file.
	 * @return A fully initialized <code>Configuration</code> instance.
	 * @throws IOException
	 *             When reading or parsing the "etm.yml" file fails.
	 */
	private static Configuration loadConfiguration(File configDir) throws IOException {
		if (!configDir.exists()) {
			throw new FileNotFoundException(configDir.getAbsolutePath());
		}
		File configFile = new File(configDir, "etm.yml");
		if (!configFile.exists() || !configFile.isFile() || !configFile.canRead()) {
			throw new FileNotFoundException(configFile.getAbsolutePath());
		}
		Configuration configuration = null;
		try (Reader reader = new FileReader(configFile);) {
			Yaml yaml = new Yaml();
			configuration = yaml.loadAs(reader, Configuration.class);
		}
		addEnvironmentToConfigruation(configuration, null);
		return configuration;
	}

	private static void addEnvironmentToConfigruation(Object configurationInstance, String prefix) {
		Field[] fields = configurationInstance.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (field.getType().getName().startsWith("com.jecstar.")) {
				// A nested configuration class.
				try {
					addEnvironmentToConfigruation(field.get(configurationInstance), prefix == null ? field.getName() : prefix + "." + field.getName());
				} catch (IllegalArgumentException | IllegalAccessException e) {
				}
			} else {
				String value = System.getenv(prefix == null ? field.getName() : prefix + "." + field.getName());
				if (value == null) {
					continue;
				}
				try {
					if (field.getType().equals(String.class)) {
						field.set(configurationInstance, value);
					} else if (field.getType().equals(Boolean.class)){
						field.set(configurationInstance, new Boolean(value));
					} else if (field.getType().equals(Integer.class)) {
						field.set(configurationInstance, new Integer(value));
					} else if (field.getType().equals(File.class)) {
						field.set(configurationInstance, new File(value));
					} 
				} catch (IllegalArgumentException | IllegalAccessException e) {
				}
			}
		}
	}
}
