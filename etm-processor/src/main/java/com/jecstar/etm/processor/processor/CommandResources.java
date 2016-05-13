package com.jecstar.etm.processor.processor;

import java.io.Closeable;
import java.util.List;

import com.jecstar.etm.domain.Endpoint;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.jecstar.etm.server.core.domain.EndpointConfiguration;

public interface CommandResources extends Closeable {

	public <T> T getPersister(CommandType commandType);
	
	public void loadEndpointConfig(List<Endpoint> endpoints, EndpointConfiguration endpointConfiguration);
	
}
