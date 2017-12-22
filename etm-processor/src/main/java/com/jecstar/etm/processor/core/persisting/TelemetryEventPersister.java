package com.jecstar.etm.processor.core.persisting;

import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.server.core.domain.converter.TelemetryEventConverter;

public interface TelemetryEventPersister<Event extends TelemetryEvent<Event>, Converter extends TelemetryEventConverter<String, Event>> {

	void persist(Event event, Converter converter);

}
