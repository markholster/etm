package com.jecstar.etm.domain.writer.json;

import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.writer.TelemetryEventTags;

public class MessagingTelemetryEventWriterJsonImpl extends AbstractJsonTelemetryEventWriter<MessagingTelemetryEvent> {

    @Override
    String getType() {
        return TelemetryEventTags.EVENT_TYPE_MESSAGING;
    }

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
