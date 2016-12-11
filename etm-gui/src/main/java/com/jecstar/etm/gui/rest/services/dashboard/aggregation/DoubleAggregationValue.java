package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

import java.text.Format;

import com.jecstar.etm.domain.writers.json.JsonWriter;

public class DoubleAggregationValue extends AbstractAggregationValue<Double> {

	private static final Double DEFAULT_MISSING_VALUE = 0d;
	private final Double value;
	
	private Double missingValue = DEFAULT_MISSING_VALUE;

	public DoubleAggregationValue(String label, Double value) {
		super(label);
		this.value = value;
	}

	@Override
	public Double getValue() {
		if (hasValidValue()) {
			return this.value;
		}
		if (isValidValue(this.missingValue)) {
			return this.missingValue;
		}
		return null;
	}
	
	@Override
	public Double getMissingValue() {
		return this.missingValue;
	}
	
	@Override
	public void setMissingValue(Double missingValue) {
		this.missingValue = missingValue;
	}

	@Override
	public String getValueAsString(Format format) {
		if (hasValidValue()) {
			return format.format(getValue());
		}
		if (isValidValue(this.missingValue)) {
			return format.format(this.missingValue);
		}
		return "?";
	}

	@Override
	public void appendValueToJsonBuffer(JsonWriter jsonWriter, StringBuilder buffer, boolean firstElement) {
		jsonWriter.addDoubleElementToJsonBuffer("value", getValue(), buffer, firstElement);
	}

	@Override
	public boolean hasValidValue() {
		return isValidValue(this.value);
	}
	
	private boolean isValidValue(Double value) {
		return value != null && !Double.isNaN(value) && !Double.isInfinite(value);
	}

}
