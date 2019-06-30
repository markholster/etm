package com.jecstar.etm.server.core.domain.cluster.notifier;

import com.jecstar.etm.server.core.converter.JsonNamespace;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;

@JsonNamespace(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER)
public class BusinessEventNotifier extends Notifier {

    @Override
    public ConnectionTestResult testConnection(DataRepository dataRepository) {
        return ConnectionTestResult.OK;
    }
}
