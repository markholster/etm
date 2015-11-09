package com.jecstar.etm.processor.repository;

import com.jecstar.etm.processor.TelemetryEvent;

public interface TelemetryEventRepository {

	void persistTelemetryEvent(TelemetryEvent telemetryEvent);
	
	void findEndpointConfig(String endpoint, EndpointConfigResult result);
}
