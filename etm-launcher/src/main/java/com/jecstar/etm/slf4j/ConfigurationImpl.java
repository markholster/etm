package com.jecstar.etm.slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class ConfigurationImpl implements Configuration {
	
	public static List<String> endpointUrls = new ArrayList<String>();
	public static long pushInterval = 5000;
	public static int maxRequestsPerBatch = 1000;
	public static int numberOfWorkers = 1;
	public static String rootLogLevel = "INFO";
	public static TreeMap<String, String> loggers = new TreeMap<String, String>();
	public static String applicationName = "Enterprise Telemetry Monitor";
	public static String applicationVersion = null;
	public static String applicationInstance = null;
	public static String principalName = System.getProperty("user.name");
	
	@Override
	public List<String> getEndpointUrls() {
		return endpointUrls;
	}

	@Override
	public long getPushInterval() {
		return pushInterval;
	}

	@Override
	public int getMaxRequestsPerBatch() {
		return maxRequestsPerBatch;
	}

	@Override
	public int getNumberOfWorkers() {
		return numberOfWorkers;
	}

	@Override
	public String getRootLogLevel() {
		return rootLogLevel;
	}

	@Override
	public TreeMap<String, String> getLoggers() {
		return loggers;
	}

	@Override
	public String getApplicationName() {
		return applicationName;
	}

	@Override
	public String getApplicationVersion() {
		return applicationVersion;
	}

	@Override
	public String getApplicationInstance() {
		return applicationInstance;
	}

	@Override
	public String getPrincipalName() {
		return principalName;
	}

}
