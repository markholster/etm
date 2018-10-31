package com.jecstar.etm.server.core.domain.cluster.notifier.converter;

import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.domain.cluster.notifier.BusinessEventNotifier;
import com.jecstar.etm.server.core.domain.cluster.notifier.EmailNotifier;
import com.jecstar.etm.server.core.domain.cluster.notifier.Notifier;
import com.jecstar.etm.server.core.domain.cluster.notifier.SnmpNotifier;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;

import java.util.Map;

public class NotifierConverter extends JsonEntityConverter<Notifier> {

    @SuppressWarnings("unchecked")
    public NotifierConverter() {
        super(f -> {
            // First remove the notifier namespace
            Map<String, Object> dataMap = (Map<String, Object>) f.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER);
            switch (Notifier.NotifierType.safeValueOf((String) dataMap.get(Notifier.NOTIFIER_TYPE))) {
                case EMAIL:
                    return new EmailNotifier();
                case ETM_BUSINESS_EVENT:
                    return new BusinessEventNotifier();
                case SNMP:
                    return new SnmpNotifier();
                default:
                    throw new IllegalArgumentException((String) dataMap.get(Notifier.NOTIFIER_TYPE));

            }
        });
    }

}
