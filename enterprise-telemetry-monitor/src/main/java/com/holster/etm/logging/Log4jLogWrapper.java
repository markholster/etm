package com.holster.etm.logging;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Log wrapper that wraps the <a href="http://logging.apache.org/log4j/">log4j</a> logging system.
 * 
 * @author Mark Holster
 */
public class Log4jLogWrapper extends AbstractDelegateLogWrapper {

    /**
     * The log4j logger.
     */
    private final Logger logger;

    /**
     * Constructs a new <code>Log4jLogWrapper</code> instance.
     * 
     * @param loggerName
     *            The name of the logger.
     */
    public Log4jLogWrapper(String loggerName) {
        super(loggerName);
        this.logger = Logger.getLogger(getLoggerName());
    }

    @Override
    public boolean isDebugLevelEnabled() {
        return this.logger.isEnabledFor(Level.DEBUG);
    }

    @Override    public boolean isErrorLevelEnabled() {
        return this.logger.isEnabledFor(Level.ERROR);
    }

    @Override    public boolean isFatalLevelEnabled() {
        return this.logger.isEnabledFor(Level.FATAL);
    }

    @Override    public boolean isInfoLevelEnabled() {
        return this.logger.isEnabledFor(Level.INFO);
    }

    @Override    public boolean isWarningLevelEnabled() {
        return this.logger.isEnabledFor(Level.WARN);
    }

    @Override
    protected void logDelegatedDebugMessage(String message, Throwable throwable) {
        this.logger
                .log(getLoggingClassName(), Level.DEBUG, message, throwable);
    }

    @Override
    protected void logDelegatedErrorMessage(String message, Throwable throwable) {
        this.logger
                .log(getLoggingClassName(), Level.ERROR, message, throwable);
    }

    @Override
    protected void logDelegatedFatalMessage(String message, Throwable throwable) {
        this.logger
                .log(getLoggingClassName(), Level.FATAL, message, throwable);
    }

    @Override
    protected void logDelegatedInfoMessage(String message, Throwable throwable) {
        this.logger.log(getLoggingClassName(), Level.INFO, message, throwable);
    }

    @Override
    protected void logDelegatedWarningMessage(String message, Throwable throwable) {
        this.logger.log(getLoggingClassName(), Level.WARN, message, throwable);
    }
    
    @Override
    public boolean isManaged() {
    	return true;
    }

    @Override
    public void setToDebugLevel() {
        this.logger.setLevel(Level.DEBUG);
    }

    @Override
    public void setToErrorLevel() {
        this.logger.setLevel(Level.ERROR);
    }

    @Override
    public void setToFatalLevel() {
        this.logger.setLevel(Level.FATAL);
    }

    @Override
    public void setToInfoLevel() {
        this.logger.setLevel(Level.INFO);
    }

    @Override
    public void setToWarningLevel() {
        this.logger.setLevel(Level.WARN);
    }

    /**
     * Gives all currently known logger names.
     * 
     * @return All currently known logger names.
     */
    @SuppressWarnings("unchecked")
    public static List<String> getCurrentLoggerNames() {
        Enumeration<Logger> currentLoggers = LogManager.getCurrentLoggers();
        List<String> loggers = new ArrayList<String>();
        while (currentLoggers.hasMoreElements()) {
            loggers.add(currentLoggers.nextElement().getName());
        }
        return loggers;
    }

}
