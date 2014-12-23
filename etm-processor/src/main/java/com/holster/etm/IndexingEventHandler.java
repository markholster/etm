package com.holster.etm;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;

import com.lmax.disruptor.EventHandler;

public class IndexingEventHandler implements EventHandler<TelemetryEvent> {

	private final SolrServer server;
	private final long ordinal;
	private final long numberOfConsumers;
	private final SolrInputDocument document;
	
	public IndexingEventHandler(final SolrServer server, final long ordinal, final long numberOfConsumers) {
		this.server = server;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		this.document = new SolrInputDocument();
	}

	@Override
	public void onEvent(TelemetryEvent event, long sequence, boolean endOfBatch)
			throws Exception {
		if (event.ignore || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
		this.document.clear();
		this.document.addField("id", event.id.toString());
		if (event.application != null) {
			this.document.addField("application", event.application);
		}
		if (event.content != null) {
			this.document.addField("content", event.content);
		}
		if (event.correlationId != null) {
			this.document.addField("correlationId", event.correlationId);
		}
		if (event.creationTime != null) {
			this.document.addField("creationTime", event.creationTime);
		}
		if (event.direction != null) {
			this.document.addField("direction", event.direction.name());
		}
		if (event.endpoint != null) {
			this.document.addField("endpoint", event.endpoint);
		}
		if (event.expiryTime != null) {
			this.document.addField("expirtyTime", event.expiryTime);
		}
		if (event.name != null) {
			this.document.addField("name", event.name);
		}
		if (event.sourceCorrelationId != null) {
			this.document.addField("sourceCorrelationId", event.sourceCorrelationId);
		}
		if (event.sourceId != null) {
			this.document.addField("sourceId", event.sourceId);
		}
		if (event.transactionId != null) {
			this.document.addField("transactionId", event.transactionId);
		}
		if (event.transactionName != null) {
			this.document.addField("transactionName", event.transactionName);
		}
		if (event.type != null) {
			this.document.addField("type", event.type.name());
		}
		if (event.responseTime != 0) {
			this.document.addField("responseTime", event.responseTime);
		}
//		this.server.add(this.document);
	}

}
