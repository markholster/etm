package com.jecstar.etm.core.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

public class Slf4jLogWrapper extends AbstractDelegateLogWrapper {

    /**
     * The slf4j logger.
     */
    private final Logger logger;
    
    /**
     * The logger class name.
     */
    private final String FQCN = AbstractDelegateLogWrapper.class.getName();

    /**
     * Constructs a new <code>Slf4jLogWrapper</code> instance.
     * 
     * @param loggerName
     *            The name of the logger.
     */
    public Slf4jLogWrapper(String loggerName) {
        super(loggerName);
        this.logger = LoggerFactory.getLogger(loggerName);
    }

	@Override
    public boolean isDebugLevelEnabled() {
	    return this.logger.isDebugEnabled();
    }

	@Override
    public boolean isInfoLevelEnabled() {
	    return this.logger.isInfoEnabled();
    }

	@Override
    public boolean isWarningLevelEnabled() {
	    return this.logger.isWarnEnabled();
    }

	@Override
    public boolean isErrorLevelEnabled() {
	    return this.logger.isErrorEnabled();
    }

	@Override
    public boolean isFatalLevelEnabled() {
	    return this.logger.isErrorEnabled();
    }

	@Override
    public boolean isManaged() {
	    return false;
    }

	@Override
    public void setToDebugLevel() {
    }

	@Override
    public void setToInfoLevel() {
    }

	@Override
    public void setToWarningLevel() {
    }

	@Override
    public void setToErrorLevel() {
    }

	@Override
    public void setToFatalLevel() {
    }

	@Override
    protected void logDelegatedDebugMessage(String message, Throwable throwable) {
		if (this.logger instanceof LocationAwareLogger) {
			 ((LocationAwareLogger)this.logger).log(null, FQCN, LocationAwareLogger.DEBUG_INT, message, null, throwable);
		} else {
			this.logger.debug(message, throwable);
		}
    }

	@Override
    protected void logDelegatedInfoMessage(String message, Throwable throwable) {
		if (this.logger instanceof LocationAwareLogger) {
			 ((LocationAwareLogger)this.logger).log(null, FQCN, LocationAwareLogger.INFO_INT, message, null, throwable);
		} else {
			this.logger.info(message, throwable);
		}
    }

	@Override
    protected void logDelegatedWarningMessage(String message, Throwable throwable) {
		if (this.logger instanceof LocationAwareLogger) {
			 ((LocationAwareLogger)this.logger).log(null, FQCN, LocationAwareLogger.WARN_INT, message, null, throwable);
		} else {
			this.logger.warn(message, throwable);
		}
	}

	@Override
    protected void logDelegatedErrorMessage(String message, Throwable throwable) {
		if (this.logger instanceof LocationAwareLogger) {
			 ((LocationAwareLogger)this.logger).log(null, FQCN, LocationAwareLogger.ERROR_INT, message, null, throwable);
		} else {
			this.logger.error(message, throwable);
		}
	}

	@Override
    protected void logDelegatedFatalMessage(String message, Throwable throwable) {
		if (this.logger instanceof LocationAwareLogger) {
			 ((LocationAwareLogger)this.logger).log(null, FQCN, LocationAwareLogger.ERROR_INT, message, null, throwable);
		} else {
			this.logger.error(message, throwable);
		}
	}
	
}