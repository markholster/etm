package com.jecstar.etm.core.domain.converter;

import com.jecstar.etm.domain.TelemetryEvent;

public interface TelemetryEventConverter<T, Event extends TelemetryEvent<Event>> {

	T convert(Event telemetryEvent);
	void convert(T content, Event telemetryEvent);
	
	TelemetryEventConverterTags getTags();
}
