package com.holster.etm.processor.repository;

import java.util.concurrent.TimeUnit;

import com.holster.etm.processor.TelemetryEvent;

public interface TelemetryEventRepository {

	void persistTelemetryEvent(TelemetryEvent telemetryEvent, TimeUnit statisticsTimeUnit);

	void findParent(String sourceId, CorrelationBySourceIdResult result);
	
	void findEndpointConfig(String endpoint, EndpointConfigResult result, long cacheExpiryTime);
}
