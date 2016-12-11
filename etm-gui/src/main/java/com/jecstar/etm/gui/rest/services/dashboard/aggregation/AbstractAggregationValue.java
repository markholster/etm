package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

import java.text.Format;

import com.jecstar.etm.domain.writers.json.JsonWriter;

public abstract class AbstractAggregationValue<T> implements AggregationValue<T> {

	private final String label;
	private boolean percentage;

	protected AbstractAggregationValue(String label) {
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return this.label;
	}
	
	@Override
	public void appendToJsonBuffer(JsonWriter jsonWriter, Format format, StringBuilder buffer, boolean firstElement) {
		jsonWriter.addStringElementToJsonBuffer("label", getLabel(), buffer, firstElement);
		appendValueToJsonBuffer(jsonWriter, buffer, false); 
		jsonWriter.addStringElementToJsonBuffer("value_as_string", getValueAsString(format) + (isPercentage() ? "%" : ""), buffer, false);
	}
	
	@Override
	public AggregationValue<T> setPercentage(boolean percentage) {
		this.percentage = percentage;
		return this;
	}
	
	@Override
	public boolean isPercentage() {
		return this.percentage;
	}
	
}
