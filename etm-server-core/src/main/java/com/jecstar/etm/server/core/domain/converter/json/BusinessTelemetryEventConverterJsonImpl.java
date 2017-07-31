package com.jecstar.etm.server.core.domain.converter.json;

import java.util.Map;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.writer.json.BusinessTelemetryEventWriterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.TelemetryEventConverter;

public class BusinessTelemetryEventConverterJsonImpl extends BusinessTelemetryEventWriterJsonImpl implements TelemetryEventConverter<String, BusinessTelemetryEvent> {

	private final TelemetryEventJsonConverter<BusinessTelemetryEvent> converter = new TelemetryEventJsonConverter<>();
	
	@Override
	public BusinessTelemetryEvent read(String content) {
		return read(this.converter.toMap(content));
	}
	
	@Override
	public void read(String content, BusinessTelemetryEvent event) {
		read(this.converter.toMap(content), event);
	}

	@Override
	public BusinessTelemetryEvent read(Map<String, Object> valueMap) {
		BusinessTelemetryEvent event = new BusinessTelemetryEvent();
		read(valueMap, event);
		return event;
	}

	@Override
	public void read(Map<String, Object> valueMap, BusinessTelemetryEvent event) {
		this.converter.convert(valueMap, event);
	}

}
