package com.jecstar.etm.processor.core.persisting;

import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.domain.writers.TelemetryEventWriter;

public interface TelemetryEventPersister<Event extends TelemetryEvent<Event>, Writer extends TelemetryEventWriter<String, Event>> {

	void persist(Event event, Writer writer);

}
