package com.jecstar.etm.domain.builder;

import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;

import java.time.Instant;

public class MessagingTelemetryEventBuilder extends TelemetryEventBuilder<MessagingTelemetryEvent, MessagingTelemetryEventBuilder> {

    public MessagingTelemetryEventBuilder() {
        super(new MessagingTelemetryEvent());
    }

    public MessagingTelemetryEventBuilder setExpiry(Instant expiry) {
        this.event.expiry = expiry;
        return this;
    }

    public MessagingTelemetryEventBuilder setMessagingEventType(MessagingEventType messagingEventType) {
        this.event.messagingEventType = messagingEventType;
        return this;
    }
}
