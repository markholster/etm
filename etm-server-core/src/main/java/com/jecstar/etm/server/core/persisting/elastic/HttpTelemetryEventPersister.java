package com.jecstar.etm.server.core.persisting.elastic;

import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.HttpTelemetryEvent.HttpEventType;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.HttpTelemetryEventConverterJsonImpl;
import com.jecstar.etm.server.core.persisting.TelemetryEventPersister;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import java.util.HashMap;

public class HttpTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
        implements TelemetryEventPersister<HttpTelemetryEvent, HttpTelemetryEventConverterJsonImpl> {

    public HttpTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
        super(bulkProcessor, etmConfiguration);
    }

    @Override
    public void persist(HttpTelemetryEvent event, HttpTelemetryEventConverterJsonImpl converter) {
        var indexRequestBuilder = createIndexRequest(event.id).setSource(converter.write(event, false, false), XContentType.JSON);

        if (event.id == null) {
            // An event without an id can never be an update.
            bulkProcessor.add(indexRequestBuilder.build());
            licenseRateLimiter.addRequestUnits(indexRequestBuilder.calculateIndexRequestUnits()).throttle();
        } else {
            var parameters = new HashMap<String, Object>();
            parameters.put("source", XContentHelper.convertToMap(new BytesArray(converter.write(event, true, false)), false, XContentType.JSON).v2());
            parameters.put("event_id", event.id);
            bulkProcessor.add(createUpdateRequest(event.id)
                    .setScript(new Script(ScriptType.STORED, null, "etm_update-event", parameters))
                    .setUpsert(indexRequestBuilder)
                    .build()
            );
            licenseRateLimiter.addRequestUnits(indexRequestBuilder.calculateIndexRequestUnits()).throttle();

            // Set the correlation on the parent.
            if (HttpEventType.RESPONSE.equals(event.httpEventType) && event.correlationId != null) {
                bulkProcessor.add(createUpdateRequest(event.correlationId)
                        .setScript(new Script(ScriptType.STORED, null, "etm_update-request-with-response", parameters))
                        .setUpsert("{}", XContentType.JSON)
                        .setScriptedUpsert(true)
                        .build()
                );
                licenseRateLimiter.addRequestUnits(correlation_request_units);
            } else {
                setCorrelationOnParent(event);
            }
        }
    }
}
