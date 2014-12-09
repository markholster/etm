package com.holster.etm.repository;

import com.holster.etm.TelemetryEvent;

public interface TelemetryEventRepository {

	void persistTelemetryEvent(TelemetryEvent telemetryEvent);

	void findParent(String sourceId, CorrelationBySourceIdResult result);
}
