package com.jecstar.etm.server.core.persisting.elastic;

import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.LogTelemetryEventConverterJsonImpl;
import com.jecstar.etm.server.core.persisting.TelemetryEventPersister;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.common.xcontent.XContentType;

public class LogTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
        implements TelemetryEventPersister<LogTelemetryEvent, LogTelemetryEventConverterJsonImpl> {

    public LogTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
        super(bulkProcessor, etmConfiguration);
    }

    @Override
    public void persist(LogTelemetryEvent event, LogTelemetryEventConverterJsonImpl converter) {
        var indexRequestBuilder = createIndexRequest(event.id).setSource(converter.write(event, false, false), XContentType.JSON);
        bulkProcessor.add(indexRequestBuilder.build());
        setCorrelationOnParent(event);
        licenseRateLimiter.addRequestUnits(indexRequestBuilder.calculateIndexRequestUnits()).throttle();
    }
}
