package com.jecstar.etm.launcher.slf4j;

import java.net.InetAddress;
import java.util.TreeMap;

public class LogConfiguration {

	public static String rootLogLevel = "INFO";
	public static TreeMap<String, String> loggers = new TreeMap<String, String>();
	public static String applicationName = "Enterprise Telemetry Monitor";
	public static String applicationVersion = System.getProperty("app.version");
	public static String applicationInstance = null;
	public static String principalName = System.getProperty("user.name");
	public static InetAddress hostAddress = null;
	
	public String getRootLogLevel() {
		return rootLogLevel;
	}

	public TreeMap<String, String> getLoggers() {
		return loggers;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public String getApplicationVersion() {
		return applicationVersion;
	}

	public String getApplicationInstance() {
		return applicationInstance;
	}

	public String getPrincipalName() {
		return principalName;
	}

	public InetAddress getHostAddress() {
		return hostAddress;
	}
	
	String getLogLevel(String loggerName) {
		if (loggerName == null) {
			return getRootLogLevel();
		}
		String specificLevel = getLoggers().get(loggerName);
		if (specificLevel != null) {
			return specificLevel;
		}
		int ix = loggerName.lastIndexOf("$");
		if (ix != -1) {
			return getLogLevel(loggerName.substring(0, ix));
		}
		ix = loggerName.lastIndexOf(".");
		if (ix != -1) {
			return getLogLevel(loggerName.substring(0, ix));
		}
		return getRootLogLevel();
	}
	
	// TODO create a method to update the loglevel of a logger on the fly.
}
