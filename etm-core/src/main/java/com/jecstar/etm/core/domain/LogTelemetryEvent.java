package com.jecstar.etm.core.domain;

public class LogTelemetryEvent extends TelemetryEvent<LogTelemetryEvent> {
	
	public String logLevel;

	@Override
	public LogTelemetryEvent initialize() {
		super.internalInitialize();
		logLevel = null;
		return this;
	}

	@Override
	public LogTelemetryEvent initialize(LogTelemetryEvent copy) {
		super.internalInitialize(copy);
		this.initialize();
		this.logLevel = copy.logLevel;
		return this;
	}

}
