package com.holster.etm.processor.processor;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import com.holster.etm.processor.TelemetryEvent;
import com.lmax.disruptor.EventHandler;

public class IndexingEventHandler implements EventHandler<TelemetryEvent>, Closeable {

	private final int nrOfDocumentsPerRequest = 50;
	private final SolrServer server;
	private final long ordinal;
	private final long numberOfConsumers;
	private final List<SolrInputDocument> documents = new ArrayList<SolrInputDocument>(nrOfDocumentsPerRequest);
	private int docIx = -1;
	
	public IndexingEventHandler(final SolrServer server, final long ordinal, final long numberOfConsumers) {
		this.server = server;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		for (int i=0; i < this.nrOfDocumentsPerRequest; i++) {
			this.documents.add(new SolrInputDocument());
		}
	}

	@Override
	public void onEvent(TelemetryEvent event, long sequence, boolean endOfBatch)
			throws Exception {
		if (event.ignore || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
//		long start = System.nanoTime();
		this.docIx++;
		SolrInputDocument document = this.documents.get(this.docIx);
		document.clear();
		document.addField("id", event.id.toString());
		if (event.application != null) {
			document.addField("application", event.application);
		}
		if (event.content != null) {
			document.addField("content", event.content);
		}
		if (event.correlationId != null) {
			document.addField("correlationId", event.correlationId);
		}
		if (event.creationTime != null) {
			document.addField("creationTime", event.creationTime);
		}
		if (event.direction != null) {
			document.addField("direction", event.direction.name());
		}
		if (event.endpoint != null) {
			document.addField("endpoint", event.endpoint);
		}
		if (event.expiryTime != null) {
			document.addField("expirtyTime", event.expiryTime);
		}
		if (event.name != null) {
			document.addField("name", event.name);
		}
		if (event.sourceCorrelationId != null) {
			document.addField("sourceCorrelationId", event.sourceCorrelationId);
		}
		if (event.sourceId != null) {
			document.addField("sourceId", event.sourceId);
		}
		if (event.transactionId != null) {
			document.addField("transactionId", event.transactionId);
		}
		if (event.transactionName != null) {
			document.addField("transactionName", event.transactionName);
		}
		if (event.type != null) {
			document.addField("type", event.type.name());
		}
		if (event.creationTime.getTime() != 0 && event.correlationCreationTime.getTime() != 0) {
			long responseTime = event.correlationCreationTime.getTime() - event.creationTime.getTime();
			document.addField("responseTime", responseTime);
		}
		if (this.docIx == this.nrOfDocumentsPerRequest - 1) {
			this.server.add(this.documents, 60000);
			this.docIx = -1;
		}
//		Statistics.indexingTime.addAndGet(System.nanoTime() - start);
	}

	@Override
    public void close() throws IOException {
		if (this.docIx != -1) {
			try {
	            this.server.add(this.documents.subList(0, this.docIx), 60000);
            } catch (SolrServerException e) {
	            // TODO error handling
            }
		}
    }

}
