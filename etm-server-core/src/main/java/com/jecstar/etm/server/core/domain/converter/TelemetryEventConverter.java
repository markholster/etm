package com.jecstar.etm.server.core.domain.converter;

import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.domain.writer.TelemetryEventWriter;

import java.util.Map;

public interface TelemetryEventConverter<T, Event extends TelemetryEvent<Event>> extends TelemetryEventWriter<T, Event> {

	Event read(T content, String id);
	void read(T content, Event event, String id);
	Event read(Map<String, Object> valueMap, String id);
	void read(Map<String, Object> valueMap, Event event, String id);
}
