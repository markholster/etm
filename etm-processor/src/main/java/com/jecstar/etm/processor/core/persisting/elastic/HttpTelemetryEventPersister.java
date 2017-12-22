package com.jecstar.etm.processor.core.persisting.elastic;

import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.HttpTelemetryEvent.HttpEventType;
import com.jecstar.etm.domain.writer.json.HttpTelemetryEventWriterJsonImpl;
import com.jecstar.etm.processor.core.persisting.TelemetryEventPersister;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import java.util.HashMap;
import java.util.Map;

public class HttpTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
		implements TelemetryEventPersister<HttpTelemetryEvent, HttpTelemetryEventWriterJsonImpl> {

	public HttpTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		super(bulkProcessor, etmConfiguration);
	}

	@Override
	public void persist(HttpTelemetryEvent event, HttpTelemetryEventWriterJsonImpl writer) {
		IndexRequest indexRequest = createIndexRequest(event.id).source(writer.write(event, false), XContentType.JSON);

		if (event.id == null) {
		    // An event without an id can never be an update.
            bulkProcessor.add(indexRequest);
        } else {
            Map<String, Object> parameters =  new HashMap<>();
            parameters.put("source", XContentHelper.convertToMap(new BytesArray(writer.write(event, true)), false, XContentType.JSON).v2());
            bulkProcessor.add(createUpdateRequest(event.id)
                    .script(new Script(ScriptType.STORED, null, "etm_update-event", parameters))
                    .upsert(indexRequest));

            // Set the correlation on the parent.
            if (HttpEventType.RESPONSE.equals(event.httpEventType) && event.correlationId != null) {
                bulkProcessor.add(createUpdateRequest(event.correlationId)
                        .script(new Script(ScriptType.STORED, null, "etm_update-request-with-response", parameters))
                        .upsert("{}", XContentType.JSON)
                        .scriptedUpsert(true));
            } else {
                setCorrelationOnParent(event);
            }
        }
	}
}
