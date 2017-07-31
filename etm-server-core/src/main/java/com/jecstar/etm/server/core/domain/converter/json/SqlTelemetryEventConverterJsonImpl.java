package com.jecstar.etm.server.core.domain.converter.json;

import java.util.Map;

import com.jecstar.etm.domain.SqlTelemetryEvent;
import com.jecstar.etm.domain.SqlTelemetryEvent.SqlEventType;
import com.jecstar.etm.domain.writer.json.SqlTelemetryEventWriterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.TelemetryEventConverter;

public class SqlTelemetryEventConverterJsonImpl extends SqlTelemetryEventWriterJsonImpl implements TelemetryEventConverter<String, SqlTelemetryEvent> {

	private final TelemetryEventJsonConverter<SqlTelemetryEvent> converter = new TelemetryEventJsonConverter<>();
	
	@Override
	public SqlTelemetryEvent read(String content) {
		return read(this.converter.toMap(content));
	}
	
	@Override
	public void read(String content, SqlTelemetryEvent event) {
		read(this.converter.toMap(content), event);
	}

	@Override
	public SqlTelemetryEvent read(Map<String, Object> valueMap) {
		SqlTelemetryEvent event = new SqlTelemetryEvent();
		read(valueMap, event);
		return event;
	}

	@Override
	public void read(Map<String, Object> valueMap, SqlTelemetryEvent event) {
		this.converter.convert(valueMap, event);
		event.sqlEventType =  SqlEventType.safeValueOf(this.converter.getString(getTags().getSqlEventTypeTag(), valueMap));
	}

}
