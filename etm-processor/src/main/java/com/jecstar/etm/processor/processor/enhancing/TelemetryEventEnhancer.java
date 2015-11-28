package com.jecstar.etm.processor.processor.enhancing;

import com.jecstar.etm.core.domain.TelemetryEvent;

public interface TelemetryEventEnhancer<Event extends TelemetryEvent<Event>> {

	void enhance(Event event);
}
