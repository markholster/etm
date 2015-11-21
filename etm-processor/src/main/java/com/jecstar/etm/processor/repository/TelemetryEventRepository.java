package com.jecstar.etm.processor.repository;

import com.jecstar.etm.core.TelemetryEvent;

public interface TelemetryEventRepository {

	void persistTelemetryEvent(TelemetryEvent telemetryEvent);
	
	void findEndpointConfig(String endpoint, EndpointConfigResult result);

	void flushDocuments();
}
