package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

import java.text.Format;

import com.jecstar.etm.domain.writers.json.JsonWriter;

public class DoubleAggregationValue extends AbstractAggregationValue<Double> {

	private final Double value;

	public DoubleAggregationValue(String label, Double value) {
		super(label);
		this.value = value;
	}

	@Override
	public Double getValue() {
		return this.value;
	}

	@Override
	public String getValueAsString(Format format) {
		return Double.isNaN(getValue()) ? "?" : format.format(getValue());
	}

	@Override
	public void addValueToBuffer(JsonWriter jsonWriter, StringBuilder buffer, boolean firstElement) {
		jsonWriter.addDoubleElementToJsonBuffer("value", getValue(), buffer, firstElement);
	}

}
