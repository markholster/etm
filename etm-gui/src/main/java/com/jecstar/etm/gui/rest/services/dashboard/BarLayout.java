package com.jecstar.etm.gui.rest.services.dashboard;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.jecstar.etm.domain.writers.json.JsonWriter;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.AggregationValue;

public class BarLayout {

	private Map<String, Map<String, AggregationValue<?>>> values = new HashMap<>();
	
	public void addValueToSerie(String seriesName, String key, AggregationValue<?> value) {
		if (!this.values.containsKey(seriesName)) {
			this.values.put(seriesName, new HashMap<String, AggregationValue<?>>());
		}
		Map<String, AggregationValue<?>> map = this.values.get(seriesName);
		map.put(key, value);
	}
	
	public void appendAsArrayToJsonBuffer(JsonWriter jsonWriter, StringBuilder buffer, boolean firstElement) {
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("[");
		boolean firstSerie = true;
		for (Entry<String, Map<String, AggregationValue<?>>> serie: this.values.entrySet()) {
			if (!firstSerie) {
				buffer.append(",");
			}
			buffer.append("{");
			jsonWriter.addStringElementToJsonBuffer("key", serie.getKey(), buffer, true);
			buffer.append(",\"values\": [");
			boolean firstItem = true;
			for (Entry<String, AggregationValue<?>> item : serie.getValue().entrySet()) {
				if (!firstItem) {
					buffer.append(",");
				}
				buffer.append("{");
				jsonWriter.addStringElementToJsonBuffer("label", item.getKey(), buffer, true);
				item.getValue().appendValueToJsonBuffer(jsonWriter, buffer, false);
				buffer.append("}");
				firstItem = false;
			}
			buffer.append("]");
			buffer.append("}");
			firstSerie = false;
		}
		buffer.append("]");
	}

}
