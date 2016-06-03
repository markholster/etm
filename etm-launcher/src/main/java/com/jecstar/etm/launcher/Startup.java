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
			System.err.println("Configuration file not found: " + e.getMessage());
			return;
		} catch (YamlException e) {
			System.err.println("Error reading configuration file: " + e.getMessage());
			return;
		} catch (UnknownHostException e) {
			System.err.println("Invalid binding address: " + e.getMessage());
			return;
		}
	}		
	
	private static Configuration loadConfiguration(File configDir) throws FileNotFoundException, YamlException {
		if (!configDir.exists()) {
			throw new FileNotFoundException(configDir.getAbsolutePath());
		}
		File configFile = new File(configDir, "etm.yml");
		if (!configFile.exists() || !configFile.isFile() || !configFile.canRead()) {
			throw new FileNotFoundException(configFile.getAbsolutePath());
		}
		YamlReader reader = new YamlReader(new FileReader(configFile));
		reader.getConfig().setBeanProperties(false);
		return reader.read(Configuration.class);
	}
}
