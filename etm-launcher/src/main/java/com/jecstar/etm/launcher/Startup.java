package com.jecstar.etm.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.slf4j.ConfigurationImpl;

public class Startup {

	public static void main(String[] args) {
		CommandLineParameters commandLineParameters = new CommandLineParameters(args);
		if (!commandLineParameters.isProceedNormalStartup()) {
			return;
		}
		try {
			Configuration configuration = loadConfiguration(new File(commandLineParameters.getConfigDirectory()));
			ConfigurationImpl.loggers.putAll(configuration.logging.loggers);
			ConfigurationImpl.rootLogLevel = configuration.logging.rootLogger;
			ConfigurationImpl.applicationInstance = configuration.instanceName;
			ConfigurationImpl.hostAddress = InetAddress.getByName(configuration.bindingAddress);
			// TODO set the application version.		
			new Launcher().launch(commandLineParameters, configuration);
		} catch (FileNotFoundException e) {
			System.err.println("Configuration file not found.");
			return;
		} catch (YamlException e) {
			System.err.println("Error reading configuration file.");
			return;
		} catch (UnknownHostException e) {
			System.err.println("Invalid binding address.");
			return;
		}
	}		
	
	private static Configuration loadConfiguration(File configDir) throws FileNotFoundException, YamlException {
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
		reader.getConfig().setBeanProperties(false);
		return reader.read(Configuration.class);
	}
}
