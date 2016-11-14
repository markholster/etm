package com.jecstar.etm.processor.core.persisting.elastic;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService.ScriptType;

import com.jecstar.etm.domain.SqlTelemetryEvent;
import com.jecstar.etm.domain.SqlTelemetryEvent.SqlEventType;
import com.jecstar.etm.domain.writers.json.SqlTelemetryEventWriterJsonImpl;
import com.jecstar.etm.processor.core.persisting.TelemetryEventPersister;
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

public class SqlTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
		implements TelemetryEventPersister<SqlTelemetryEvent, SqlTelemetryEventWriterJsonImpl> {

	public SqlTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		super(bulkProcessor, etmConfiguration);
	}

	@Override
	public void persist(SqlTelemetryEvent event, SqlTelemetryEventWriterJsonImpl writer) {
		IndexRequest indexRequest = createIndexRequest(event.id)
				.source(writer.write(event));
		Map<String, Object> parameters =  new HashMap<>();
		parameters.put("source", indexRequest.sourceAsMap());
		bulkProcessor.add(createUpdateRequest(event.id)
					.script(new Script("etm_update-event", ScriptType.STORED, "painless", parameters))
					.upsert(indexRequest));
		if (SqlEventType.RESULTSET.equals(event.sqlEventType) && event.correlationId != null) {
			bulkProcessor.add(createUpdateRequest(event.correlationId)
					.script(new Script("etm_update-request-with-response", ScriptType.STORED, "painless", parameters))
					.upsert("{}")
					.scriptedUpsert(true));
		}			
	}

	@Override
	protected String getElasticTypeName() {
		return ElasticSearchLayout.ETM_EVENT_INDEX_TYPE_SQL;
	}

}
