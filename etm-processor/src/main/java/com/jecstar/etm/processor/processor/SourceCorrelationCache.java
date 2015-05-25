package com.jecstar.etm.processor.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jecstar.etm.core.util.ObjectUtils;
import com.jecstar.etm.processor.TelemetryEvent;

public class SourceCorrelationCache {

	private final Map<String, List<TelemetryEvent>> sourceCorrelations = new HashMap<String, List<TelemetryEvent>>();
	private final Object mutex = new Object();
	
	public TelemetryEvent getBySourceId(String sourceId) {
		synchronized (this.mutex) {
			List<TelemetryEvent> results = this.sourceCorrelations.get(sourceId);
			if (results != null) {
				return results.get(0);
			}
		}
		return null;
	}

	public TelemetryEvent getBySourceIdAndApplication(String sourceId, String application) {
		synchronized (this.mutex) {
			List<TelemetryEvent> results = this.sourceCorrelations.get(sourceId);
			if (results != null) {
				for (TelemetryEvent result : results) {
					if (ObjectUtils.equalsNullProof(application, result.application)) {
						return result;
					}
				}
			}
		}
		return null;
	}

	public void addTelemetryEvent(TelemetryEvent event) {
		TelemetryEvent clone = event.clone();
		synchronized (this.mutex) {
			if (this.sourceCorrelations.containsKey(event.sourceId)) {
				List<TelemetryEvent> list = this.sourceCorrelations.get(event.sourceId);
				list.add(event);
			} else {
				List<TelemetryEvent> list = new ArrayList<TelemetryEvent>();
				list.add(clone);
				this.sourceCorrelations.put(event.sourceId, list);
			}
		}
	}

	public void removeTelemetryEvent(TelemetryEvent event) {
		synchronized (this.mutex) {
			List<TelemetryEvent> list = this.sourceCorrelations.get(event.sourceId);
			if (list != null) {
				list.remove(event);
				if (list.size() == 0) {
					this.sourceCorrelations.remove(event.sourceId);
				}
			}
		}
	}

}
