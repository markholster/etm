package com.jecstar.etm.processor;

import com.jecstar.etm.core.domain.TelemetryEvent;

public class TelemetryCommand {

	public enum CommandType {
		EVENT
	}

	public CommandType commandType;

	public TelemetryEvent event = new TelemetryEvent();

	public void initialize() {
		this.commandType = null;
		this.event.initialize();
	}

	public void initialize(TelemetryCommand telemetryCommand) {
		initialize();
		this.commandType = telemetryCommand.commandType;
		switch (telemetryCommand.commandType) {
		case EVENT:
			this.event.initialize(telemetryCommand.event);
			break;
		default:
			break;
		}
	}
}
