package com.jecstar.etm.server.core.domain.converter.json;

import java.util.Map;

import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.writers.json.MessagingTelemetryEventWriterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.TelemetryEventConverter;

public class MessagingTelemetryEventConverterJsonImpl extends MessagingTelemetryEventWriterJsonImpl implements TelemetryEventConverter<String, MessagingTelemetryEvent> {

	private final TelemetryEventJsonConverter<MessagingTelemetryEvent> converter = new TelemetryEventJsonConverter<>();
	
	@Override
	public MessagingTelemetryEvent read(String content) {
		return read(this.converter.toMap(content));
	}
	
	@Override
	public void read(String content, MessagingTelemetryEvent event) {
		read(this.converter.toMap(content), event);
	}

	@Override
	public MessagingTelemetryEvent read(Map<String, Object> valueMap) {
		MessagingTelemetryEvent event = new MessagingTelemetryEvent();
		read(valueMap, event);
		return event;
	}

	@Override
	public void read(Map<String, Object> valueMap, MessagingTelemetryEvent event) {
		this.converter.convert(valueMap, event);
		event.expiry = this.converter.getZonedDateTime(getTags().getExpiryTag(), valueMap);
		event.messagingEventType = MessagingEventType.safeValueOf(this.converter.getString(getTags().getMessagingEventTypeTag(), valueMap));
	}

}
