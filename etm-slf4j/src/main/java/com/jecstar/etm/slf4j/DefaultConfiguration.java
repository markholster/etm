package com.jecstar.etm.slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class DefaultConfiguration implements Configuration {

	private List<String> endpointUrls = new ArrayList<String>();
	private TreeMap<String, String> loggers = new TreeMap<String, String>();
	
	public DefaultConfiguration() {
	}
	
	@Override
	public List<String> getEndpointUrls() {
		return this.endpointUrls;
	}

	@Override
	public long getPushInterval() {
		return 5000;
	}

	@Override
	public int getMaxRequestsPerBatch() {
		return 1000;
	}

	@Override
	public int getNumberOfWorkers() {
		return 1;
	}

	@Override
	public String getRootLogLevel() {
		return "INFO";
	}

	@Override
	public TreeMap<String, String> getLoggers() {
		return this.loggers;
	}

	@Override
	public String getApplicationName() {
		return null;
	}

	@Override
	public String getApplicationVersion() {
		return null;
	}

	@Override
	public String getApplicationInstance() {
		return null;
	}

	@Override
	public String getPrincipalName() {
		return null;
	}

}
