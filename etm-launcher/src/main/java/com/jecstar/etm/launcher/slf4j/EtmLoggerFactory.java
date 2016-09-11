package com.jecstar.etm.launcher.slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.elasticsearch.client.Client;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import com.jecstar.etm.server.core.configuration.EtmConfiguration;

public class EtmLoggerFactory implements ILoggerFactory {

	private final ConcurrentMap<String, Logger> loggerMap;
	private final LogConfiguration logConfiguration;

	private static LogBulkProcessorWrapper bulkProcessorWrapper;
	
	public static void initialize(LogBulkProcessorWrapper bulkProcessorWrapper) {
		EtmLoggerFactory.bulkProcessorWrapper = bulkProcessorWrapper;
	}

	public static void setClient(Client elasticClient) {
		bulkProcessorWrapper.setClient(elasticClient);
	}
	public static void setConfiguration(EtmConfiguration etmConfiguration) {
		bulkProcessorWrapper.setConfiguration(etmConfiguration);
	}
	
	public EtmLoggerFactory() {
		this.loggerMap = new ConcurrentHashMap<String, Logger>();
		this.logConfiguration = new LogConfiguration();
	}
	
	@Override
	public Logger getLogger(String name) {
		Logger etmLogger = this.loggerMap.get(name);
		if (etmLogger != null) {
			return etmLogger;
		} else {
			Logger newInstance = new EtmLogger(name, this.logConfiguration, bulkProcessorWrapper);
			Logger oldInstance = loggerMap.putIfAbsent(name, newInstance);
			return oldInstance == null ? newInstance : oldInstance;
		}
	}

	public static void close() {
		if (bulkProcessorWrapper != null) {
			bulkProcessorWrapper.close();
		}
	}

}
