package com.holster.etm.processor.repository;

import com.holster.etm.processor.TelemetryEvent;

public interface TelemetryEventRepository {

	void persistTelemetryEvent(TelemetryEvent telemetryEvent);

	void findParent(String sourceId, CorrelationBySourceIdResult result);
	
	void findEndpointConfig(String endpoint, EndpointConfigResult result);
}
