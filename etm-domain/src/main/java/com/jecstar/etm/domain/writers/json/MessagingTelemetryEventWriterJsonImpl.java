package com.jecstar.etm.domain.writers.json;

import com.jecstar.etm.domain.MessagingTelemetryEvent;

public class MessagingTelemetryEventWriterJsonImpl extends AbstractJsonTelemetryEventWriter<MessagingTelemetryEvent> {

	@Override
	boolean doWrite(MessagingTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
		boolean added = !firstElement;
		if (event.expiry != null) {
			added = this.jsonWriter.addLongElementToJsonBuffer(getTags().getExpiryTag(), event.expiry.toInstant().toEpochMilli(), buffer, !added) || added;
		}
		if (event.messagingEventType != null) {
			added = this.jsonWriter.addStringElementToJsonBuffer(getTags().getMessagingEventTypeTag(), event.messagingEventType.name(), buffer, !added) || added;
		}
		return added;
	}

}
