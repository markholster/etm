package com.jecstar.etm.processor.repository;

import java.io.Closeable;

import com.jecstar.etm.core.domain.EndpointConfiguration;
import com.jecstar.etm.core.domain.TelemetryEvent;

public interface TelemetryEventRepository extends Closeable {

	void persistTelemetryEvent(TelemetryEvent telemetryEvent);
	
	void findEndpointConfig(String endpoint, EndpointConfiguration result);
	
}
