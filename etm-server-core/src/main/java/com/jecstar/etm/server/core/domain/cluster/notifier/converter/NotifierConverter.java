package com.jecstar.etm.server.core.domain.cluster.notifier.converter;

import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.domain.cluster.notifier.Notifier;

public class NotifierConverter extends JsonEntityConverter<Notifier> {

    public NotifierConverter() {
        super(Notifier::new);
    }

}
