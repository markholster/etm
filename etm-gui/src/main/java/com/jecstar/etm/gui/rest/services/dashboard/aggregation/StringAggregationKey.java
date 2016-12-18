package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

public class StringAggregationKey implements AggregationKey {
	
	private final String key;

	public StringAggregationKey(String key) {
		this.key = key;
	}
	
	@Override
	public int compareTo(AggregationKey o) {
		return this.key.compareTo(o.getKeyAsString());
	}

	@Override
	public String getKeyAsString() {
		return this.key;
	}

	@Override
	public int getLength() {
		return this.key.length();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StringAggregationKey) {
			StringAggregationKey other = (StringAggregationKey) obj;
			return this.key.equals(other);
		}
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return this.key.hashCode();
	}
	
}
