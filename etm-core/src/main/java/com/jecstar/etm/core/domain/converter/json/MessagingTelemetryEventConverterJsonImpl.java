package com.jecstar.etm.core.domain.converter.json;

import java.util.Map;

import com.jecstar.etm.core.domain.MessagingTelemetryEvent;
import com.jecstar.etm.core.domain.MessagingTelemetryEvent.MessagingEventType;

public class MessagingTelemetryEventConverterJsonImpl extends AbstractJsonTelemetryEventConverter<MessagingTelemetryEvent> {

	@Override
	boolean doConvert(MessagingTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
		boolean added = !firstElement;
		if (event.expiry != null) {
			added = addLongElementToJsonBuffer(getTags().getExpiryTag(), event.expiry.toInstant().toEpochMilli(), buffer, !added) || added;
		}
		if (event.messagingEventType != null) {
			added = addStringElementToJsonBuffer(getTags().getMessagingEventTypeTag(), event.messagingEventType.name(), buffer, !added) || added;
		}
		if (!event.readingEndpointHandlers.isEmpty()) {
			if (added) {
				buffer.append(", ");
			}
			buffer.append("\"" + getTags().getReadingEndpointHandlersTag() + "\": [");
			added = false;
			for (int i = 0; i < event.readingEndpointHandlers.size(); i++) {
				added = addEndpointHandlerToJsonBuffer(event.readingEndpointHandlers.get(i), buffer, i == 0 ? true : !added, getTags()) || added;
			}
			buffer.append("]");
		}
		return added;
	}

	@Override
	void doConvert(MessagingTelemetryEvent telemetryEvent, Map<String, Object> valueMap) {
		telemetryEvent.expiry = getZonedDateTime(getTags().getExpiryTag(), valueMap);
		telemetryEvent.messagingEventType = MessagingEventType.safeValueOf(getString(getTags().getMessagingEventTypeTag(), valueMap));
		getArray(getTags().getReadingEndpointHandlersTag(), valueMap).forEach(c -> telemetryEvent.readingEndpointHandlers.add(createEndpointFormValueMapHandler(c)));
	}

}
