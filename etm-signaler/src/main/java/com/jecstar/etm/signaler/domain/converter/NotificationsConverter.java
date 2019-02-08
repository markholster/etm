package com.jecstar.etm.signaler.domain.converter;

import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;
import com.jecstar.etm.signaler.domain.Notifications;

public class NotificationsConverter extends NestedObjectConverter<Notifications> {

    public NotificationsConverter() {
        super(f -> new Notifications());
    }
}
