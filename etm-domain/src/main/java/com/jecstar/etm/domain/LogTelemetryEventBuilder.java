package com.jecstar.etm.domain;

public class LogTelemetryEventBuilder extends TelemetryEventBuilder<LogTelemetryEvent, LogTelemetryEventBuilder> {

	public LogTelemetryEventBuilder() {
		super(new LogTelemetryEvent());
	}
	
	public LogTelemetryEventBuilder setLogLevel(String logLevel) {
		this.event.logLevel = logLevel;
		return this;
	}
	
	public LogTelemetryEventBuilder setStackTrace(String stackTrace) {
		this.event.stackTrace = stackTrace;
		return this;
	}

}
