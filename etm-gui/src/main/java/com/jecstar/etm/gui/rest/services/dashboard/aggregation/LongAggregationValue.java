package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

import java.text.Format;

import com.jecstar.etm.domain.writer.json.JsonWriter;

public class LongAggregationValue extends AbstractAggregationValue<Long> {

	private static final Long DEFAULT_MISSING_VALUE = 0L;
	private final Long value;
	private Long missingValue = DEFAULT_MISSING_VALUE;

	public LongAggregationValue(String label, Long value) {
		super(label);
		this.value = value;
	}

	@Override
	public Long getValue() {
		if (hasValidValue()) {
			return this.value;
		}
		if (isValidValue(this.missingValue)) {
			return this.missingValue;
		}
		return null;
	}
	
	@Override
	public Long getMissingValue() {
		return this.missingValue;
	}
	
	@Override
	public void setMissingValue(Long missingValue) {
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
		jsonWriter.addLongElementToJsonBuffer("value", getValue(), buffer, firstElement);
	}

	@Override
	public boolean hasValidValue() {
		return isValidValue(this.value);
	}
	
	private boolean isValidValue(Long value) {
		return value != null;
	}

}
