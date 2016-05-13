package com.jecstar.etm.server.core.domain.converter.json;

import java.util.Map;

import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.domain.writers.json.LogTelemetryEventWriterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.TelemetryEventConverter;

public class LogTelemetryEventConverterJsonImpl extends LogTelemetryEventWriterJsonImpl implements TelemetryEventConverter<String, LogTelemetryEvent> {

	private final TelemetryEventJsonConverter<LogTelemetryEvent> converter = new TelemetryEventJsonConverter<>();
	
	@Override
	public LogTelemetryEvent read(String content) {
		return read(this.converter.toMap(content));
	}
	
	@Override
	public void read(String content, LogTelemetryEvent event) {
		read(this.converter.toMap(content), event);	
	}

	@Override
	public LogTelemetryEvent read(Map<String, Object> valueMap) {
		LogTelemetryEvent event = new LogTelemetryEvent();
		read(valueMap, event);
		return event;
	}

	@Override
	public void read(Map<String, Object> valueMap, LogTelemetryEvent event) {
		this.converter.convert(valueMap, event);
		event.logLevel = this.converter.getString(getTags().getLogLevelTag(), valueMap);
		event.stackTrace = this.converter.getString(getTags().getStackTraceTag(), valueMap);
	}

}
