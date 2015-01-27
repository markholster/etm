package com.holster.etm.scheduler.retention;

import java.util.UUID;

import org.apache.solr.client.solrj.StreamingResponseCallback;
import org.apache.solr.common.SolrDocument;

import com.datastax.driver.core.Session;

public class RemovingCallbackHandler extends StreamingResponseCallback {

	private final Session session;
	private final String keyspace;

	public RemovingCallbackHandler(Session session, String keyspace) {
		this.session = session;
		this.keyspace = keyspace;
	}

	@Override
	public void streamSolrDocument(SolrDocument doc) {
		UUID id = UUID.fromString((String) doc.get("id"));
		System.out.println("Removing " + id);
		// TODO remove event from solr + cassandra
	}

	@Override
	public void streamDocListInfo(long numFound, long start, Float maxScore) {
	}

}
