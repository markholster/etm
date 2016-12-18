package com.jecstar.etm.gui.rest.services.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jecstar.etm.domain.writers.json.JsonWriter;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.AggregationKey;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.AggregationValue;

public class BarOrLineLayout {

	public enum SortOrder {ASC, DESC}
	
	private Map<String, Map<AggregationKey, AggregationValue<?>>> values = new HashMap<>();
	private SortOrder sortOrder = SortOrder.ASC;
	
	public void addValueToSerie(String seriesName, AggregationKey key, AggregationValue<?> value) {
		if (!this.values.containsKey(seriesName)) {
			this.values.put(seriesName, new HashMap<AggregationKey, AggregationValue<?>>());
		}
		Map<AggregationKey, AggregationValue<?>> map = this.values.get(seriesName);
		map.put(key, value);
	}
	
	public BarOrLineLayout setSortOrder(SortOrder sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}
	
	public void appendAsArrayToJsonBuffer(JsonWriter jsonWriter, StringBuilder buffer, boolean firstElement) {
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("[");
		boolean firstSerie = true;
		for (Entry<String, Map<AggregationKey, AggregationValue<?>>> serie: this.values.entrySet()) {
			if (!firstSerie) {
				buffer.append(",");
			}
			int labelLength = 0;
			buffer.append("{");
			jsonWriter.addStringElementToJsonBuffer("key", serie.getKey(), buffer, true);
			buffer.append(",\"values\": [");
			boolean firstItem = true;
			List<AggregationKey> keys = new ArrayList<>(serie.getValue().keySet());
			Collections.sort(keys);
			if (SortOrder.DESC.equals(this.sortOrder)) {
				Collections.reverse(keys);
			}
			for (AggregationKey key : keys) {
				if (!firstItem) {
					buffer.append(",");
				}
				if (key.getLength() > labelLength) {
					labelLength = key.getLength();
				}
				buffer.append("{");
				AggregationValue<?> aggregationValue = serie.getValue().get(key);
				jsonWriter.addStringElementToJsonBuffer("label", key.getKeyAsString(), buffer, true);
				aggregationValue.appendValueToJsonBuffer(jsonWriter, buffer, false);
				buffer.append("}");
				firstItem = false;
			}
			buffer.append("]");
			buffer.append(", \"max_label_length\": " + labelLength);
			buffer.append("}");
			firstSerie = false;
		}
		buffer.append("]");
	}

}
