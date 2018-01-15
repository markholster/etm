package com.jecstar.etm.processor.core;

import com.jecstar.etm.domain.Endpoint;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.jecstar.etm.server.core.domain.EndpointConfiguration;

import java.io.Closeable;
import java.util.List;

public interface CommandResources extends Closeable {

	<T> T getPersister(CommandType commandType);
	
	void loadEndpointConfig(List<Endpoint> endpoints, EndpointConfiguration endpointConfiguration);
	
}
