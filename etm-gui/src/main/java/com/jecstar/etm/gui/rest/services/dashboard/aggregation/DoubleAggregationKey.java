package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

import java.text.Format;

public class DoubleAggregationKey implements AggregationKey {

	private final Double key;
	private final Format format;

	public DoubleAggregationKey(Double key, Format format) {
		this.key = key;
		this.format = format;
	}
	
	@Override
	public int compareTo(AggregationKey o) {
		if (o instanceof DoubleAggregationKey) {
			DoubleAggregationKey other = (DoubleAggregationKey) o;
			return this.key.compareTo(other.key);
		}
		return 0;
	}

	@Override
	public String getKeyAsString() {
		return this.format.format(key);
	}

	@Override
	public int getLength() {
		return getKeyAsString().length();
	}
}
