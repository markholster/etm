package com.jecstar.etm.processor.repository;

import java.io.Closeable;

import com.jecstar.etm.core.TelemetryMessageEvent;

public interface TelemetryEventRepository extends Closeable {

	void persistTelemetryMessageEvent(TelemetryMessageEvent telemetryMessageEvent);
	
	void findEndpointConfig(String endpoint, EndpointConfigResult result, long cacheExpiryTime);
}
