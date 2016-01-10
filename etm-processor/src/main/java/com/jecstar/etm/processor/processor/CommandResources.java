package com.jecstar.etm.processor.processor;

import java.io.Closeable;

import com.jecstar.etm.core.domain.EndpointConfiguration;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;

public interface CommandResources extends Closeable {

	public <T> T getPersister(CommandType commandType);
	
	public void loadEndpointConfig(String endpoint, EndpointConfiguration endpointConfiguration);
	
}
