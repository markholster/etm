package com.jecstar.etm.processor.processor;

import java.util.HashMap;
import java.util.Map;

import com.jecstar.etm.core.TelemetryEvent;

public class SourceCorrelationCache {

	private final Map<String, TelemetryEvent> sourceCorrelations = new HashMap<String, TelemetryEvent>();
	
	public TelemetryEvent getBySourceId(String sourceId) {
		return this.sourceCorrelations.get(sourceId);
	}

	/**
	 * Adds a <code>TelemetryEvent</code> to the cache.
	 * 
	 * @param event
	 *            The <code>TelemetryEvent</code> to add.
	 */
	public synchronized void addTelemetryEvent(TelemetryEvent event) {
		TelemetryEvent current = this.sourceCorrelations.get(event.sourceId);
		if (current != null) {
			event.id = current.id;
		} else {
			this.sourceCorrelations.put(event.sourceId, event);
		}
	}

	public synchronized void removeTelemetryEvent(TelemetryEvent event) {
		this.sourceCorrelations.remove(event.sourceId);
	}

}
