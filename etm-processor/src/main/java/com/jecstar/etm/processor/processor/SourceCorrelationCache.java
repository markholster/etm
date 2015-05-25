package com.jecstar.etm.processor.processor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.jecstar.etm.core.TelemetryEvent;

public class SourceCorrelationCache {

	private final Map<String, TelemetryEvent> sourceCorrelations = Collections.synchronizedMap(new HashMap<String, TelemetryEvent>());
	
	public TelemetryEvent getBySourceId(String sourceId) {
		return this.sourceCorrelations.get(sourceId);
	}

	public void addTelemetryEvent(TelemetryEvent event) {
		this.sourceCorrelations.put(event.sourceId, event);
	}

	public void removeTelemetryEvent(TelemetryEvent event) {
		this.sourceCorrelations.remove(event.sourceId);
	}

}
