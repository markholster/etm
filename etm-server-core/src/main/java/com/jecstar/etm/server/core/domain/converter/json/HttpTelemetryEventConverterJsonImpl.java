package com.jecstar.etm.server.core.domain.converter.json;

import java.util.Map;

import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.HttpTelemetryEvent.HttpEventType;
import com.jecstar.etm.domain.writers.json.HttpTelemetryEventWriterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.TelemetryEventConverter;

public class HttpTelemetryEventConverterJsonImpl extends HttpTelemetryEventWriterJsonImpl implements TelemetryEventConverter<String, HttpTelemetryEvent> {

	private final TelemetryEventJsonConverter<HttpTelemetryEvent> converter = new TelemetryEventJsonConverter<>();
	
	@Override
	public HttpTelemetryEvent read(String content) {
		return read(this.converter.toMap(content));
	}
	
	@Override
	public void read(String content, HttpTelemetryEvent event) {
		read(this.converter.toMap(content), event);
	}

	@Override
	public HttpTelemetryEvent read(Map<String, Object> valueMap) {
		HttpTelemetryEvent event = new HttpTelemetryEvent();
		read(valueMap, event);
		return event;
	}

	@Override
	public void read(Map<String, Object> valueMap, HttpTelemetryEvent event) {
		this.converter.convert(valueMap, event);
		event.httpEventType =  HttpEventType.safeValueOf(this.converter.getString(getTags().getHttpEventTypeTag(), valueMap));
	}

}
