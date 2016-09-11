package com.jecstar.etm.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.yaml.snakeyaml.Yaml;

import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.launcher.slf4j.EtmLoggerFactory;
import com.jecstar.etm.launcher.slf4j.LogBulkProcessorWrapper;
import com.jecstar.etm.launcher.slf4j.LogConfiguration;

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
			EtmLoggerFactory.initialize(new LogBulkProcessorWrapper());
			new Launcher().launch(commandLineParameters, configuration);
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
	
	private static Configuration loadConfiguration(File configDir) throws IOException {
		if (!configDir.exists()) {
			throw new FileNotFoundException(configDir.getAbsolutePath());
		}
		File configFile = new File(configDir, "etm.yml");
		if (!configFile.exists() || !configFile.isFile() || !configFile.canRead()) {
			throw new FileNotFoundException(configFile.getAbsolutePath());
		}
		try (Reader reader = new FileReader(configFile);) {
			Yaml yaml = new Yaml();
			return yaml.loadAs(reader, Configuration.class);
		} 
	}
}
