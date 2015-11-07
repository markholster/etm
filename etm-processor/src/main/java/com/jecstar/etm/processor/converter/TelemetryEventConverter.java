package com.jecstar.etm.processor.converter;

import com.jecstar.etm.processor.TelemetryEvent;

public interface TelemetryEventConverter<T> {

	T convert(TelemetryEvent telemetryEvent);
	void convert(T content, TelemetryEvent telemetryEvent);
	
	TelemetryEventConverterTags getTags();
}
