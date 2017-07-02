package com.jecstar.etm.processor.core.persisting.elastic;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import com.jecstar.etm.domain.SqlTelemetryEvent;
import com.jecstar.etm.domain.SqlTelemetryEvent.SqlEventType;
import com.jecstar.etm.domain.writers.json.SqlTelemetryEventWriterJsonImpl;
import com.jecstar.etm.processor.core.persisting.TelemetryEventPersister;
import com.jecstar.etm.server.core.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

public class SqlTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
		implements TelemetryEventPersister<SqlTelemetryEvent, SqlTelemetryEventWriterJsonImpl> {

	public SqlTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		super(bulkProcessor, etmConfiguration);
	}

	@Override
	public void persist(SqlTelemetryEvent event, SqlTelemetryEventWriterJsonImpl writer) {
		IndexRequest indexRequest = createIndexRequest(event.id)
				.source(writer.write(event), XContentType.JSON);
		Map<String, Object> parameters =  new HashMap<>();
		parameters.put("source", XContentHelper.convertToMap(new BytesArray(writer.write(event, true)), false, XContentType.JSON).v2());
		bulkProcessor.add(createUpdateRequest(event.id)
					.script(new Script(ScriptType.STORED, "painless", "etm_update-event", parameters))
					.upsert(indexRequest));
		if (SqlEventType.RESULTSET.equals(event.sqlEventType) && event.correlationId != null) {
			bulkProcessor.add(createUpdateRequest(event.correlationId)
					.script(new Script(ScriptType.STORED, "painless", "etm_update-request-with-response", parameters))
					.upsert("{}", XContentType.JSON)
					.scriptedUpsert(true));
		} else {
			setCorrelationOnParent(event);
		}			
	}

	@Override
	protected String getElasticTypeName() {
		return ElasticsearchLayout.ETM_EVENT_INDEX_TYPE_SQL;
	}

}
