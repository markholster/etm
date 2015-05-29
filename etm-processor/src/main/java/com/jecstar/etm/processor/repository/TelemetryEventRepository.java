package com.jecstar.etm.processor.repository;

import java.util.concurrent.TimeUnit;

import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.TelemetryMessageEvent;

public interface TelemetryEventRepository {

	void persistTelemetryEvent(TelemetryMessageEvent telemetryMessageEvent, TimeUnit statisticsTimeUnit);
	
	TelemetryEvent findBySourceId(String sourceId);
	
	void findEndpointConfig(String endpoint, EndpointConfigResult result, long cacheExpiryTime);
}
