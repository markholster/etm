package com.jecstar.etm.core;

public class TelemetryCommand {

	public enum CommandType {
		MESSAGE_EVENT, FLUSH_DOCUMENTS
	}

	public CommandType commandType;

	public TelemetryMessageEvent messageEvent = new TelemetryMessageEvent();

	public void initialize() {
		this.commandType = null;
		this.messageEvent.initialize();
	}

	public void initialize(TelemetryCommand telemetryCommand) {
		initialize();
		this.commandType = telemetryCommand.commandType;
		switch (telemetryCommand.commandType) {
		case MESSAGE_EVENT:
			this.messageEvent.initialize(telemetryCommand.messageEvent);
			break;
		default:
			break;
		}
	}
}
