package com.jecstar.etm.server.core.domain.converter.json;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.writer.json.BusinessTelemetryEventWriterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.TelemetryEventConverter;

import java.util.Map;

public class BusinessTelemetryEventConverterJsonImpl extends BusinessTelemetryEventWriterJsonImpl implements TelemetryEventConverter<String, BusinessTelemetryEvent> {

	private final TelemetryEventJsonConverter<BusinessTelemetryEvent> converter = new TelemetryEventJsonConverter<>();

    @Override
    public String write(BusinessTelemetryEvent event, boolean includeId) {
        return super.write(event, includeId);
    }

    @Override
    protected boolean doWrite(BusinessTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
	    boolean added = this.converter.addDatabaseFields(buffer, event, firstElement);
        return super.doWrite(event, buffer, !added);
    }

    @Override
	public BusinessTelemetryEvent read(String content, String id) {
	    return read(this.converter.toMap(content), id);
	}
	
	@Override
	public void read(String content, BusinessTelemetryEvent event, String id) {
		read(this.converter.toMap(content), event, id);
	}

	@Override
	public BusinessTelemetryEvent read(Map<String, Object> valueMap, String id) {
		BusinessTelemetryEvent event = new BusinessTelemetryEvent();
		read(valueMap, event, id);
		return event;
	}

	@Override
	public void read(Map<String, Object> valueMap, BusinessTelemetryEvent event, String id) {
		this.converter.convert(valueMap, event, id);
	}

}
