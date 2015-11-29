package com.jecstar.etm.processor.processor.persisting;

import com.jecstar.etm.core.domain.TelemetryEvent;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverter;

public interface TelemetryEventPersister<Event extends TelemetryEvent<Event>, Converter extends TelemetryEventConverter<String, Event>> {

	void persist(Event event, Converter converter);

}
