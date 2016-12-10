package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

import java.text.Format;

import com.jecstar.etm.domain.writers.json.JsonWriter;

public interface AggregationValue<T> {

	String getLabel();
	T getValue();
	String getValueAsString(Format format);
	void addValueToBuffer(JsonWriter jsonWriter, StringBuilder buffer, boolean firstElement);
	void appendToJsonBuffer(JsonWriter jsonWriter, Format format, StringBuilder buffer, boolean firstElement);
	AggregationValue<T> setPercentage(boolean isPercentage);
	boolean isPercentage();
}
