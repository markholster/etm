package com.holster.etm.logging;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Log wrapper that wraps the <a
 * href="http://java.sun.com/j2se/1.4.2/docs/guide/util/logging/index.html">Java
 * Util Logging</a> logging system.
 * 
 * @author Mark Holster
 */
public class JULLogWrapper extends AbstractDelegateLogWrapper {

	/**
	 * The JUL logger.
	 */
	private Logger logger;

	/**
	 * JUL doesn't support a FATAL level. We have to keep track of the current
	 * level in the LogWrapper itself.
	 */
	private LogLevel currentLogLevel;

	/**
	 * Constructs a new <code>JULLogWrapper</code> instance.
	 * 
	 * @param loggerName
	 *            The name of the logger.
	 */
	public JULLogWrapper(String loggerName) {
		super(loggerName);
		this.logger = Logger.getLogger(loggerName);
		Level level = this.logger.getLevel();
		if (Level.FINE.equals(level)) {
			this.currentLogLevel = LogLevel.DEBUG;
		} else if (Level.INFO.equals(level)) {
			this.currentLogLevel = LogLevel.INFO;
		} else if (Level.WARNING.equals(level)) {
			this.currentLogLevel = LogLevel.WARNING;
		} else if (Level.SEVERE.equals(level)) {
			this.currentLogLevel = LogLevel.ERROR;
		}
	}

	@Override
	public boolean isDebugLevelEnabled() {
		return this.logger.isLoggable(Level.FINE);
	}

	@Override
	public boolean isErrorLevelEnabled() {
		if (this.logger.isLoggable(Level.SEVERE)) {
			return LogLevel.DEBUG.equals(this.currentLogLevel) 
					|| LogLevel.INFO.equals(this.currentLogLevel)
			        || LogLevel.WARNING.equals(this.currentLogLevel) 
			        || LogLevel.ERROR.equals(this.currentLogLevel)
			        || Level.ALL.equals(this.logger.getLevel())
			        || Level.CONFIG.equals(this.logger.getLevel())
			        || Level.FINER.equals(this.logger.getLevel())
			        || Level.FINEST.equals(this.logger.getLevel());
		}
		return false;
	}

	@Override
	public boolean isFatalLevelEnabled() {
		if (this.logger.isLoggable(Level.SEVERE)) {
			return LogLevel.DEBUG.equals(this.currentLogLevel) 
					|| LogLevel.INFO.equals(this.currentLogLevel)
			        || LogLevel.WARNING.equals(this.currentLogLevel) 
			        || LogLevel.ERROR.equals(this.currentLogLevel)
			        || LogLevel.FATAL.equals(this.currentLogLevel)
			        || Level.ALL.equals(this.logger.getLevel())
			        || Level.CONFIG.equals(this.logger.getLevel())
			        || Level.FINER.equals(this.logger.getLevel())
			        || Level.FINEST.equals(this.logger.getLevel());
		}
		return false;
	}

	@Override
	public boolean isInfoLevelEnabled() {
		return this.logger.isLoggable(Level.INFO);
	}

	@Override
	public boolean isWarningLevelEnabled() {
		return this.logger.isLoggable(Level.WARNING);
	}

	@Override
	protected void logDelegatedDebugMessage(String message, Throwable throwable) {
		String[] names = getCallingClassAndMethodNames();
		this.logger.logp(Level.FINE, names[0], names[1], message, throwable);
	}

	@Override
	protected void logDelegatedErrorMessage(String message, Throwable throwable) {
		String[] names = getCallingClassAndMethodNames();
		this.logger.logp(Level.SEVERE, names[0], names[1], message, throwable);
	}

	@Override
	protected void logDelegatedFatalMessage(String message, Throwable throwable) {
		String[] names = getCallingClassAndMethodNames();
		this.logger.logp(Level.SEVERE, names[0], names[1], message, throwable);
	}

	@Override
	protected void logDelegatedInfoMessage(String message, Throwable throwable) {
		String[] names = getCallingClassAndMethodNames();
		this.logger.logp(Level.INFO, names[0], names[1], message, throwable);
	}

	@Override
	protected void logDelegatedWarningMessage(String message, Throwable throwable) {
		String[] names = getCallingClassAndMethodNames();
		this.logger.logp(Level.WARNING, names[0], names[1], message, throwable);
	}

	private String[] getCallingClassAndMethodNames() {
		StackTraceElement stackTraceElement = getCallingStackTraceElement();
		if (stackTraceElement == null) {
			return new String[] { "unknown", "unknown" };
		}
		return new String[] { stackTraceElement.getClassName(), stackTraceElement.getMethodName() };
	}

	private StackTraceElement getCallingStackTraceElement() {
		Throwable throwable = new Throwable();
		throwable.fillInStackTrace();
		StackTraceElement[] stackTraceElements = throwable.getStackTrace();
		boolean loggingClassNameFound = false;
		for (StackTraceElement stackTraceElement : stackTraceElements) {
			if (stackTraceElement.getClassName().equals(getLoggingClassName())) {
				loggingClassNameFound = true;
				continue;
			}
			if (loggingClassNameFound) {
				return stackTraceElement;
			}
		}
		return null;
	}

	@Override
	public boolean isManaged() {
		return true;
	}

	@Override
	public void setToDebugLevel() {
		this.logger.setLevel(Level.FINE);
		this.currentLogLevel = LogLevel.DEBUG;
	}

	@Override
	public void setToErrorLevel() {
		this.logger.setLevel(Level.SEVERE);
		this.currentLogLevel = LogLevel.ERROR;
	}

	@Override
	public void setToFatalLevel() {
		this.logger.setLevel(Level.SEVERE);
		this.currentLogLevel = LogLevel.FATAL;
	}

	@Override
	public void setToInfoLevel() {
		this.logger.setLevel(Level.INFO);
		this.currentLogLevel = LogLevel.INFO;
	}

	@Override
	public void setToWarningLevel() {
		this.logger.setLevel(Level.WARNING);
		this.currentLogLevel = LogLevel.WARNING;
	}

	/**
	 * Gives all currently known logger names.
	 * 
	 * @return All currently known logger names.
	 */
	public static List<String> getCurrentLoggerNames() {
		Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames();
		return Collections.list(loggerNames);
	}
}
