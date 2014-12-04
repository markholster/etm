package com.holster.etm;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;

import com.lmax.disruptor.EventHandler;

public class IndexingEventHandler implements EventHandler<TelemetryEvent> {

	private final SolrServer server;
	private final long ordinal;
	private final long numberOfConsumers;
	
	public IndexingEventHandler(final SolrServer server, final long ordinal, final long numberOfConsumers) {
		this.server = server;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
	}

	@Override
	public void onEvent(TelemetryEvent event, long sequence, boolean endOfBatch)
			throws Exception {
		if (event.ignore || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
//		SolrInputDocument document = new SolrInputDocument();
//		document.addField("id", event.id.toString());
//		document.addField("application", event.application);
//		document.addField("content", event.content);
//		document.addField("endpoint", event.endpoint);
//		document.addField("eventTime", event.eventTime);
//		document.addField("sourceCorrelationId", event.sourceCorrelationId);
//		document.addField("sourceId", event.sourceId);
//		this.server.add(document);
	}

}
