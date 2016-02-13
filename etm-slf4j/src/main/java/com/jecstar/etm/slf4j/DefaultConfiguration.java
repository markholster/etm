package com.jecstar.etm.slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultConfiguration implements Configuration {

	private List<String> endpointUrls = new ArrayList<String>();
	private Map<String, String> loggers = new HashMap<String, String>();
	
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
	public Map<String, String> getLoggers() {
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
