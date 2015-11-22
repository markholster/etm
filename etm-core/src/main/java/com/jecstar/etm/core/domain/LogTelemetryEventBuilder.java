package com.jecstar.etm.core.domain;

public class LogTelemetryEventBuilder extends TelemetryEventBuilder<LogTelemetryEvent, LogTelemetryEventBuilder> {

	public LogTelemetryEventBuilder() {
		super(new LogTelemetryEvent());
	}
	
	public LogTelemetryEventBuilder setLogLevel(String logLevel) {
		this.event.logLevel = logLevel;
		return this;
	}

}
