package com.jecstar.etm.processor.processor;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.EventCommand;
import com.jecstar.etm.processor.TelemetryEvent;
import com.lmax.disruptor.EventHandler;

public class IndexingEventHandler implements EventHandler<TelemetryEvent>, Closeable {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(IndexingEventHandler.class);
	
	//TODO this should be configurable
	private final int nrOfDocumentsPerRequest = 250;
	private final SolrClient client;
	private final long ordinal;
	private final long numberOfConsumers;
	private final List<SolrInputDocument> documents = new ArrayList<SolrInputDocument>(this.nrOfDocumentsPerRequest);
	private int docIx = -1;
	private long lastAdd = 0;
	private final Timer timer;
	
	public IndexingEventHandler(final SolrClient client, final long ordinal, final long numberOfConsumers, final Timer timer) {
		this.client = client;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		for (int i=0; i < this.nrOfDocumentsPerRequest; i++) {
			this.documents.add(new SolrInputDocument());
		}
		this.timer = timer;
	}

	@Override
	public void onEvent(TelemetryEvent event, long sequence, boolean endOfBatch)
			throws Exception {
		if (event.ignore) {
			return;
		}
		if (EventCommand.FLUSH_DOCUMENTS.equals(event.eventCommand)) {
			if (this.docIx != -1 && System.currentTimeMillis() - this.lastAdd > 60000 ) {
				this.client.add(this.documents.subList(0, this.docIx + 1), 15000);
				this.docIx = -1;
				this.lastAdd = System.currentTimeMillis();
			}			
			return;
		}
		if (!EventCommand.PROCESS.equals(event.eventCommand) || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
		final Context timerContext = this.timer.time();
		try {
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
			if (event.endpoint != null) {
				document.addField("endpoint", event.endpoint);
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
			if (event.retention != null) {
				document.addField("retention", event.retention);
			}
			if (this.docIx == this.nrOfDocumentsPerRequest - 1) {
				this.client.add(this.documents, 15000);
				this.docIx = -1;
				this.lastAdd = System.currentTimeMillis();
			}
		} finally {
			timerContext.stop();
		}
	}

	@Override
    public void close() throws IOException {
		if (this.docIx != -1) {
			try {
				// TODO commitWithin time should be in configuration
	            this.client.add(this.documents.subList(0, this.docIx + 1), 60000);
            } catch (SolrServerException e) {
	            if (log.isErrorLevelEnabled()) {
	            	log.logErrorMessage("Unable to add documents to indexer.", e);
	            }
            }
		}
    }

}
