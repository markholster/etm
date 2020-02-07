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
    protected void doWrite(MessagingTelemetryEvent event, JsonBuilder builder) {
        builder.field(getTags().getExpiryTag(), event.expiry);
        if (event.messagingEventType != null) {
            builder.field(getTags().getMessagingEventTypeTag(), event.messagingEventType.name());
        }
    }

}
