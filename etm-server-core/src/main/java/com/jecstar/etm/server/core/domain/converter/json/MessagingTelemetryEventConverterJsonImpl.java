package com.jecstar.etm.server.core.domain.converter.json;

import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.writer.json.MessagingTelemetryEventWriterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.TelemetryEventConverter;

import java.util.Map;

public class MessagingTelemetryEventConverterJsonImpl extends MessagingTelemetryEventWriterJsonImpl implements TelemetryEventConverter<String, MessagingTelemetryEvent> {

	private final TelemetryEventJsonConverter<MessagingTelemetryEvent> converter = new TelemetryEventJsonConverter<>();
	
	@Override
	public MessagingTelemetryEvent read(String content, String id) {
		return read(this.converter.toMap(content), id);
	}
	
	@Override
	public void read(String content, MessagingTelemetryEvent event, String id) {
		read(this.converter.toMap(content), event, id);
	}

	@Override
	public MessagingTelemetryEvent read(Map<String, Object> valueMap, String id) {
		MessagingTelemetryEvent event = new MessagingTelemetryEvent();
		read(valueMap, event, id);
		return event;
	}

	@Override
	public void read(Map<String, Object> valueMap, MessagingTelemetryEvent event, String id) {
		this.converter.convert(valueMap, event, id);
		event.expiry = this.converter.getZonedDateTime(getTags().getExpiryTag(), valueMap);
		event.messagingEventType = MessagingEventType.safeValueOf(this.converter.getString(getTags().getMessagingEventTypeTag(), valueMap));
	}

}
