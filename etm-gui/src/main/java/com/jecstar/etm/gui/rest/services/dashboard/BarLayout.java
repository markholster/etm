package com.jecstar.etm.gui.rest.services.dashboard;

import java.util.HashMap;
import java.util.Map;

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

}
