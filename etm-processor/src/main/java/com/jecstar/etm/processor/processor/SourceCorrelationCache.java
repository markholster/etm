package com.jecstar.etm.processor.processor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.repository.CorrelationBySourceIdResult;

public class SourceCorrelationCache {

	private static final String SEPARATOR = "_!_";
	//TODO proberen dit niet in een synchronised map te plaatsen, maar bijvoorbeeld in een ConcurrentMap
	private final Map<String, CorrelationBySourceIdResult> sourceCorrelations = Collections.synchronizedMap(new HashMap<String, CorrelationBySourceIdResult>());
	
	public CorrelationBySourceIdResult getBySourceId(String sourceId) {
		Optional<String> optional = this.sourceCorrelations.keySet().stream().filter(k -> k.startsWith(sourceId + SEPARATOR)).findFirst();
		if (optional.isPresent()) {
			return this.sourceCorrelations.get(optional.get());
		}
		return null;
	}
	
	public CorrelationBySourceIdResult getBySourceIdAndApplication(String sourceId, String application) {
		return this.sourceCorrelations.get(sourceId + SEPARATOR + application);
	}
	
	public void addTelemetryEvent(TelemetryEvent event) {
		this.sourceCorrelations.put(event.sourceId + SEPARATOR + event.application, new CorrelationBySourceIdResult(event.id, event.name,
		        event.transactionId, event.transactionName, event.creationTime.getTime(), event.expiryTime.getTime(), event.slaRule));
	}

	public void removeTelemetryEvent(TelemetryEvent event) {
		this.sourceCorrelations.remove(event.sourceId + SEPARATOR + event.application == null ? "" : event.application);
	}
	
}
