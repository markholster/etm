package com.jecstar.etm.domain.writer;

import com.jecstar.etm.domain.TelemetryEvent;

public interface TelemetryEventWriter<T, Event extends TelemetryEvent<Event>> {

	T write(Event telemetryEvent);
	T write(Event telemetryEvent, boolean forUpdate);
	
	TelemetryEventTags getTags();
}
