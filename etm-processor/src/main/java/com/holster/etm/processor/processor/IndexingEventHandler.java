package com.holster.etm.processor.processor;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;

import com.holster.etm.processor.TelemetryEvent;
import com.lmax.disruptor.EventHandler;

public class IndexingEventHandler implements EventHandler<TelemetryEvent> {

	private final SolrServer server;
	private final long ordinal;
	private final long numberOfConsumers;
	private final SolrInputDocument document;
	private int count;
	
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
//		long start = System.nanoTime();
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
		if (event.creationTime.getTime() != 0 && event.correlationCreationTime.getTime() != 0) {
			long responseTime = event.correlationCreationTime.getTime() - event.creationTime.getTime();
			this.document.addField("responseTime", responseTime);
		}
		if (this.ordinal == 0 && this.count == 1000) {
			this.server.commit(false, false, true);
			this.count = 0;
		}
		this.server.add(this.document);
		this.count++;
//		Statistics.indexingTime.addAndGet(System.nanoTime() - start);
	}

}
