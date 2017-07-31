package com.jecstar.etm.server.core.domain.converter;

import java.util.Map;

import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.domain.writer.TelemetryEventWriter;

public interface TelemetryEventConverter<T, Event extends TelemetryEvent<Event>> extends TelemetryEventWriter<T, Event> {

	Event read(T content);
	void read(T content, Event event);
	Event read(Map<String, Object> valueMap);
	void read(Map<String, Object> valueMap, Event event);
}
