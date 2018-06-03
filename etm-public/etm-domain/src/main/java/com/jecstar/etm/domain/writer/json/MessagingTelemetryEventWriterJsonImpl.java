package com.jecstar.etm.domain.writer.json;

import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.writer.MessagingTelemetryEventWriter;
import com.jecstar.etm.domain.writer.TelemetryEventTags;

public class MessagingTelemetryEventWriterJsonImpl extends AbstractJsonTelemetryEventWriter<MessagingTelemetryEvent> implements MessagingTelemetryEventWriter<String> {

    @Override
    String getType() {
        return TelemetryEventTags.EVENT_OBJECT_TYPE_MESSAGING;
    }

    @Override
    protected boolean doWrite(MessagingTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
        boolean added = !firstElement;
        added = this.jsonWriter.addZonedDateTimeElementToJsonBuffer(getTags().getExpiryTag(), event.expiry, buffer, !added) || added;
        if (event.messagingEventType != null) {
            added = this.jsonWriter.addStringElementToJsonBuffer(getTags().getMessagingEventTypeTag(), event.messagingEventType.name(), buffer, !added) || added;
        }
        return added;
    }

}
