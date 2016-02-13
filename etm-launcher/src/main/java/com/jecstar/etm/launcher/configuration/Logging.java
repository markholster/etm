package com.jecstar.etm.launcher.configuration;

import java.util.HashMap;
import java.util.Map;

public class Logging {

	public String rootLogger = "INFO";
	public long interval = 5000;
	public int maxRequestsPerBatch = 1000;
	public int numberOfWorkers = 1;
	public Map<String, String> loggers = new HashMap<String, String>();

	public Logging() {
		loggers.put("nl.lala", "DEBUG");
		loggers.put("nl.loele", "DEBUG");
	}
}
