package com.jecstar.etm.processor.core.persisting.elastic;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.writers.json.MessagingTelemetryEventWriterJsonImpl;
import com.jecstar.etm.processor.core.persisting.TelemetryEventPersister;
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

public class MessagingTelemetryEventPersister extends AbstractElasticTelemetryEventPersister
		implements TelemetryEventPersister<MessagingTelemetryEvent, MessagingTelemetryEventWriterJsonImpl> {

	public MessagingTelemetryEventPersister(final BulkProcessor bulkProcessor, final EtmConfiguration etmConfiguration) {
		super(bulkProcessor, etmConfiguration);
	}

	@Override
	public void persist(MessagingTelemetryEvent event, MessagingTelemetryEventWriterJsonImpl writer) {
		IndexRequest indexRequest = createIndexRequest(event.id)
				.source(writer.write(event), XContentType.JSON);
		Map<String, Object> parameters =  new HashMap<>();
		parameters.put("source", indexRequest.sourceAsMap());
		bulkProcessor.add(createUpdateRequest(event.id)
					.script(new Script(ScriptType.STORED, "painless", "etm_update-event", parameters))
					.upsert(indexRequest));
		if (MessagingEventType.RESPONSE.equals(event.messagingEventType) && event.correlationId != null) {
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
		return ElasticSearchLayout.ETM_EVENT_INDEX_TYPE_MESSAGING;
	}
}
