package com.jecstar.etm.processor.processor;

import java.io.Closeable;

import com.jecstar.etm.processor.TelemetryCommand.CommandType;

public interface CommandResources extends Closeable {

	public <T> T getPersister(CommandType commandType);
	
	public <T> T getEnhancer(CommandType commandType);
	
}
