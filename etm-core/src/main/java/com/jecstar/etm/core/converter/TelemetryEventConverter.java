package com.jecstar.etm.core.converter;

import com.jecstar.etm.core.TelemetryEvent;

public interface TelemetryEventConverter<T> {

	T convert(TelemetryEvent telemetryEvent);
	void convert(T content, TelemetryEvent telemetryEvent);
	
	TelemetryEventConverterTags getTags();
}
