package com.jecstar.etm.processor.processor;

import java.util.HashMap;
import java.util.Map;

import com.jecstar.etm.core.domain.TelemetryEvent;

public class SourceCorrelationCache {

	private final Map<String, TelemetryEvent> sourceCorrelations = new HashMap<String, TelemetryEvent>();
	
	public TelemetryEvent getBySourceId(String id) {
		return this.sourceCorrelations.get(id);
	}

	/**
	 * Adds a <code>TelemetryEvent</code> to the cache.
	 * 
	 * @param event
	 *            The <code>TelemetryEvent</code> to add.
	 */
	public synchronized void addTelemetryEvent(TelemetryEvent event) {
		this.sourceCorrelations.put(event.id, event);
	}

	public synchronized void removeTelemetryEvent(TelemetryEvent event) {
		this.sourceCorrelations.remove(event.id);
	}

}
