package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

import java.text.Format;

import com.jecstar.etm.domain.writers.json.JsonWriter;

public class LongAggregationValue extends AbstractAggregationValue<Long> {

	private final Long value;

	public LongAggregationValue(String label, Long value) {
		super(label);
		this.value = value;
	}

	@Override
	public Long getValue() {
		return this.value;
	}

	@Override
	public String getValueAsString(Format format) {
		return format.format(getValue());
	}

	@Override
	public void addValueToBuffer(JsonWriter jsonWriter, StringBuilder buffer, boolean firstElement) {
		jsonWriter.addLongElementToJsonBuffer("value", getValue(), buffer, firstElement);
	}

}
