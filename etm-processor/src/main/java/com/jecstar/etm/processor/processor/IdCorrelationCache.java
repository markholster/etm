package com.jecstar.etm.processor.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jecstar.etm.core.util.ObjectUtils;
import com.jecstar.etm.processor.TelemetryEvent;

public class IdCorrelationCache {

	private final Map<String, List<TelemetryEvent>> idCorrelations = new HashMap<String, List<TelemetryEvent>>();
	private final Object mutex = new Object();
	
	public TelemetryEvent getBySourceId(String sourceId) {
		synchronized (this.mutex) {
			List<TelemetryEvent> results = this.idCorrelations.get(sourceId);
			if (results != null) {
				return results.get(0);
			}
		}
		return null;
	}

	public TelemetryEvent getBySourceIdAndApplication(String id, String application) {
		synchronized (this.mutex) {
			List<TelemetryEvent> results = this.idCorrelations.get(id);
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
			if (this.idCorrelations.containsKey(event.id)) {
				List<TelemetryEvent> list = this.idCorrelations.get(event.id);
				list.add(event);
			} else {
				List<TelemetryEvent> list = new ArrayList<TelemetryEvent>();
				list.add(clone);
				this.idCorrelations.put(event.id, list);
			}
		}
	}

	public void removeTelemetryEvent(TelemetryEvent event) {
		synchronized (this.mutex) {
			List<TelemetryEvent> list = this.idCorrelations.get(event.id);
			if (list != null) {
				list.remove(event);
				if (list.size() == 0) {
					this.idCorrelations.remove(event.id);
				}
			}
		}
	}

}
