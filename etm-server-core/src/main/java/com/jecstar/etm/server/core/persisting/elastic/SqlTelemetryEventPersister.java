package com.jecstar.etm.server.core.persisting.elastic;

import com.jecstar.etm.domain.SqlTelemetryEvent;
import com.jecstar.etm.domain.SqlTelemetryEvent.SqlEventType;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.SqlTelemetryEventConverterJsonImpl;
import com.jecstar.etm.server.core.persisting.TelemetryEventPersister;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import java.util.HashMap;
import java.util.Map;

public class SqlTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
        implements TelemetryEventPersister<SqlTelemetryEvent, SqlTelemetryEventConverterJsonImpl> {

    public SqlTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
        super(bulkProcessor, etmConfiguration);
    }

    @Override
    public void persist(SqlTelemetryEvent event, SqlTelemetryEventConverterJsonImpl converter) {
        IndexRequest indexRequest = createIndexRequest(event.id).source(converter.write(event, false, false), XContentType.JSON);

        if (event.id == null) {
            // An event without an id can never be an update.
            bulkProcessor.add(indexRequest);
        } else {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("source", XContentHelper.convertToMap(new BytesArray(converter.write(event, true, false)), false, XContentType.JSON).v2());
            parameters.put("event_id", event.id);
            bulkProcessor.add(createUpdateRequest(event.id)
                    .script(new Script(ScriptType.STORED, null, "etm_update-event", parameters))
                    .upsert(indexRequest));

            // Set the correlation on the parent.
            if (SqlEventType.RESULTSET.equals(event.sqlEventType) && event.correlationId != null) {
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
