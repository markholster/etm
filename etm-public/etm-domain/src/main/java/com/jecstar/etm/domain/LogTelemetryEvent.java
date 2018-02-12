package com.jecstar.etm.domain;

/**
 * A <code>TelemetryEvent</code> that describes a log statement in an application.
 */
public class LogTelemetryEvent extends TelemetryEvent<LogTelemetryEvent> {

    public String logLevel;
    public String stackTrace;

    @Override
    public LogTelemetryEvent initialize() {
        super.internalInitialize();
        this.logLevel = null;
        this.stackTrace = null;
        return this;
    }

    @Override
    public LogTelemetryEvent initialize(LogTelemetryEvent copy) {
        super.internalInitialize(copy);
        this.logLevel = copy.logLevel;
        this.stackTrace = copy.stackTrace;
        return this;
    }

}
