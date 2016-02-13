package com.jecstar.etm.slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class EtmLoggerFactory implements ILoggerFactory {

	private final ConcurrentMap<String, Logger> loggerMap;
	private final EtmLogForwarder logForwarder;

	// TODO logger configuration.
	
	public EtmLoggerFactory() {
		this.loggerMap = new ConcurrentHashMap<String, Logger>();
		this.logForwarder = EtmLogForwarder.getInstance();
	}
	
	@Override
	public Logger getLogger(String name) {
		Logger etmLogger = this.loggerMap.get(name);
		if (etmLogger != null) {
			return etmLogger;
		} else {
			Logger newInstance = new EtmLogger(this.logForwarder, name, null, null, null);
			Logger oldInstance = loggerMap.putIfAbsent(name, newInstance);
			return oldInstance == null ? newInstance : oldInstance;
		}
	}

}
