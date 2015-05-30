package com.jecstar.etm.processor.repository;

import java.util.concurrent.TimeUnit;

import com.jecstar.etm.core.TelemetryMessageEvent;

public interface TelemetryEventRepository {

	void persistTelemetryMessageEvent(TelemetryMessageEvent telemetryMessageEvent, TimeUnit statisticsTimeUnit);
	
	void findEndpointConfig(String endpoint, EndpointConfigResult result, long cacheExpiryTime);
}
