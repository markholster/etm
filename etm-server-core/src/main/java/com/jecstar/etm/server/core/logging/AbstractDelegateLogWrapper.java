/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.server.core.logging;


/**
 * <code>LogWrapper</code> that delegates the log statements to the underlying
 * logging system.
 *
 * @author Mark Holster
 */
public abstract class AbstractDelegateLogWrapper implements LogWrapper {

    /**
     * The name of the logger.
     */
    private final String loggerName;

    /**
     * The class name that "invokes" the logging statements.
     */
    private String loggingClassName;

    /**
     * Constructs a new instance of the <code>AbstractDelegateLogWrapper</code>.
     *
     * @param loggerName The name of the logger to be created.
     */
    AbstractDelegateLogWrapper(String loggerName) {
        this.loggerName = loggerName;
        setLogWrapperClass(AbstractDelegateLogWrapper.class);
    }

    @Override
    public String getName() {
        return this.loggerName;
    }

    @Override
    public void setLogWrapperClass(Class<?> clazz) {
        if (clazz != null) {
            this.loggingClassName = clazz.getName();
        } else {
            this.loggingClassName = getClass().getName();
        }
    }

    /**
     * Gives the name of the class that is delegating the logging statements.
     *
     * @return The name of the class that is delegating the logging statements.
     */
    String getLoggingClassName() {
        return this.loggingClassName;
    }

    /**
     * Gives the name of the logger.
     *
     * @return The name of the logger.
     */
    public String getLoggerName() {
        return this.loggerName;
    }

    @Override
    public void logDebugMessage(String message) {
        logDebugMessage(message, null);
    }

    @Override
    public void logDebugMessage(String message, Throwable throwable) {
        logDelegatedDebugMessage(message, throwable);
    }

    @Override
    public void logErrorMessage(String message) {
        logErrorMessage(message, null);
    }

    @Override
    public void logErrorMessage(String message, Throwable throwable) {
        logDelegatedErrorMessage(message, throwable);
    }

    @Override
    public void logFatalMessage(String message) {
        logFatalMessage(message, null);
    }

    @Override
    public void logFatalMessage(String message, Throwable throwable) {
        logDelegatedFatalMessage(message, throwable);
    }

    @Override
    public void logInfoMessage(String message) {
        logInfoMessage(message, null);
    }

    @Override
    public void logInfoMessage(String message, Throwable throwable) {
        logDelegatedInfoMessage(message, throwable);
    }

    @Override
    public void logWarningMessage(String message) {
        logWarningMessage(message, null);
    }

    @Override
    public void logWarningMessage(String message, Throwable throwable) {
        logDelegatedWarningMessage(message, throwable);
    }

    @Override
    public void setToLevel(LogLevel logLevel) {
        if (LogLevel.DEBUG.equals(logLevel)) {
            setToDebugLevel();
        } else if (LogLevel.INFO.equals(logLevel)) {
            setToInfoLevel();
        } else if (LogLevel.WARNING.equals(logLevel)) {
            setToWarningLevel();
        } else if (LogLevel.ERROR.equals(logLevel)) {
            setToErrorLevel();
        } else if (LogLevel.FATAL.equals(logLevel)) {
            setToFatalLevel();
        }
    }

    /**
     * Log a debug message to the underlying logging system.
     *
     * @param message   The log message.
     * @param throwable The throwable that's the reason to write a log statement.
     */
    protected abstract void logDelegatedDebugMessage(String message, Throwable throwable);

    /**
     * Log a info message to the underlying logging system.
     *
     * @param message   The log message.
     * @param throwable The throwable that's the reason to write a log statement.
     */
    protected abstract void logDelegatedInfoMessage(String message, Throwable throwable);

    /**
     * Log a warning message to the underlying logging system.
     *
     * @param message   The log message.
     * @param throwable The throwable that's the reason to write a log statement.
     */
    protected abstract void logDelegatedWarningMessage(String message, Throwable throwable);

    /**
     * Log a error message to the underlying logging system.
     *
     * @param message   The log message.
     * @param throwable The throwable that's the reason to write a log statement.
     */
    protected abstract void logDelegatedErrorMessage(String message, Throwable throwable);

    /**
     * Log a fatal message to the underlying logging system.
     *
     * @param message   The log message.
     * @param throwable The throwable that's the reason to write a log statement.
     */
    protected abstract void logDelegatedFatalMessage(String message, Throwable throwable);

}
