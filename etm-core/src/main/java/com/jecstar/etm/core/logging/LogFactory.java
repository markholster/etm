package com.jecstar.etm.core.logging;

import java.util.Collections;
import java.util.List;

/**
 * Class that produces <code>LogWrapper</code>'s. This factory is the entry
 * point for getting a logger to write log statements to.
 * 
 * @author Mark Holster
 */
public class LogFactory {

	/**
	 * Gives a configured <code>LogWrapper</code>. 
	 * 
	 * @param loggerName
	 *            The name that the internal logger should have.
	 * @return A <code>LogWrapper</code> instance.
	 */
	public static LogWrapper getLogger(String loggerName) {
		return new Slf4jLogWrapper(loggerName);
	}
	
	/**
	 * Gives a configured <code>LogWrapper</code>. 
	 * 
	 * @param loggerClass
	 *            The class that is going to use the <code>LogWrapper</code>.
	 * @return A <code>LogWrapper</code> instance.
	 */
	public static LogWrapper getLogger(Class<?> loggerClass) {
		return getLogger(loggerClass.getName());
	}

	/**
	 * Gives a <code>List</code> with currently known logger names. This works
	 * only with managed loggers.
	 * 
	 * @return A <code>List</code> with currently known logger names.
	 */
	public static List<String> getCurrentLoggerNames() {
		List<String> loggerNames = Collections.emptyList();
		return loggerNames;
	}

}
