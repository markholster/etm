package com.jecstar.etm.server.core.persisting.elastic;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.BusinessTelemetryEventConverterJsonImpl;
import com.jecstar.etm.server.core.persisting.TelemetryEventPersister;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.common.xcontent.XContentType;

public class BusinessTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
        implements TelemetryEventPersister<BusinessTelemetryEvent, BusinessTelemetryEventConverterJsonImpl> {

    public BusinessTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
        super(bulkProcessor, etmConfiguration);
    }

    @Override
    public void persist(BusinessTelemetryEvent event, BusinessTelemetryEventConverterJsonImpl converter) {
        var indexRequestBuilder = createIndexRequest(event.id).setSource(converter.write(event, false, false), XContentType.JSON);
        bulkProcessor.add(indexRequestBuilder.build());
        setCorrelationOnParent(event);
        licenseRateLimiter.addRequestUnits(indexRequestBuilder.calculateIndexRequestUnits()).throttle();
    }
}
