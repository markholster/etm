package com.jecstar.etm.slf4j;

import java.net.InetAddress;
import java.util.List;
import java.util.TreeMap;

/**
 * Interface for the configuration of the Enterprise Telemetry Monitor slf4j
 * logging implementation.
 * 
 * Clients can provide there own implementation of this class to fit their
 * needs. Custom implementations need to be named
 * <code>com.jecstart.etm.sfl4j.ConfigurationImpl</code>
 * 
 * @author Mark Holster
 */
public interface Configuration {

	/**
	 * Gives a list with endpoint urls for the ETM rest processor bulk api.
	 * Unless the rest processor is accessed via a proxy the pathname should end
	 * with <i>/rest/processor/event/_bulk</i>
	 *
	 * When more than one url is given the url's will be accessed in a
	 * round-robin way.
	 * 
	 * @return A list with ETM rest processor endpoint urls.
	 */
	List<String> getEndpointUrls();

	/**
	 * Gives the maximal time in milliseconds the log events should be buffered
	 * before they are send to ETM.
	 * 
	 * @return The maximal time in milliseconds the log events should be
	 *         buffered.
	 */
	long getPushInterval();

	/**
	 * Gives the maximal log events that should be added to a single batch. A
	 * batch is equal to a single POST to the ETM rest processor.
	 * 
	 * @return The maximal log events per batch.
	 */
	int getMaxRequestsPerBatch();

	/**
	 * Gives the number of worker threads that push batches to the ETM rest
	 * processor.
	 * 
	 * @return The number of worker threads.
	 */
	int getNumberOfWorkers();

	/**
	 * Gives the log level of the root logger. Must be one of <i>TRACE, DEBUG,
	 * INFO, WARNING or ERROR</i>.
	 * 
	 * @return The root log level.
	 */
	String getRootLogLevel();

	/**
	 * Gives a map that contains the logger names as key, and a corresponding
	 * log level as value. Values must be one of <i>TRACE, DEBUG, INFO, WARNING
	 * or ERROR</i>
	 * 
	 * @return The loggers and their log levels.
	 */
	TreeMap<String, String> getLoggers();

	/**
	 * Gives the name of the application that fires the log events. This
	 * property will be visible in ETM, but may be <code>null</code>.
	 * 
	 * @return The application name.
	 */
	String getApplicationName();

	/**
	 * Gives the version of the application that fires the log events. This
	 * property will be visible in ETM, but may be <code>null</code>.
	 * 
	 * @return The application version.
	 */
	String getApplicationVersion();

	/**
	 * Gives the instance of the application that fires the log events. This
	 * property will be visible in ETM, but may be <code>null</code>.
	 * 
	 * @return The application instance.
	 */
	String getApplicationInstance();

	/**
	 * Gives the principal that fires the log events. This property will be
	 * visible in ETM, but may be <code>null</code>.
	 * 
	 * @return The principal name.
	 */
	String getPrincipalName();

	/**
	 * Gives the host address of the application that fires the event. This
	 * property will be visible in ETM, but may be <code>null</code>. For
	 * performance reasons it's best to instantiate the <code>InetAddres</code>
	 * with the {@link java.net.InetAddress#getByAddress(String, byte[])}
	 * method. This prevents an expensive dns lookup.
	 * 
	 * @return The host address.
	 */
	InetAddress getHostAddress();
	
	/**
	 * Gives the log level of a given logger name.
	 * 
	 * @param loggerName The name of the logger.
	 * 
	 * @return The log level.
	 */
	default String getLogLevel(String loggerName) {
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
}
