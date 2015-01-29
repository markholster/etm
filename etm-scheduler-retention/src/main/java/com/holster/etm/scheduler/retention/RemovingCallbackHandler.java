package com.holster.etm.scheduler.retention;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.StreamingResponseCallback;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrDocument;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.holster.etm.core.configuration.EtmConfiguration;
import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;

public class RemovingCallbackHandler extends StreamingResponseCallback implements Closeable {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(RemovingCallbackHandler.class);

	private final int nrOfDocumentsPerRequest = 50;
	
	private final Session session;
	private final SolrServer solrServer;
	private final EtmConfiguration etmConfiguration;
	
	private final UpdateRequest request = new UpdateRequest();
	private final List<String> idsToDelete = new ArrayList<String>();
	private final PreparedStatement eventSelectionStatement;

	public RemovingCallbackHandler(SolrServer solrServer, Session session, EtmConfiguration etmConfiguration) {
		this.solrServer = solrServer;
		this.session = session;
		this.etmConfiguration = etmConfiguration;
		String keyspace = this.etmConfiguration.getCassandraKeyspace();
		this.eventSelectionStatement = this.session.prepare("select application, correlationData, creationTime, name, sourceId, transactionName from " + keyspace + ".telemetry_event where id = ?");
	}

	@Override
	public void streamSolrDocument(SolrDocument doc) {
		this.idsToDelete.add((String) doc.get("id"));
		if (this.idsToDelete.size() >= this.nrOfDocumentsPerRequest) {
			removeEvents();
		}
	}

	private void removeEvents() {
		if (this.idsToDelete.size() == 0) {
			return;
		}
		try {
			// First remove events from search index.
			this.request.deleteById(this.idsToDelete);
			this.request.setCommitWithin(60000);
	        this.solrServer.request(this.request);
			this.request.clear();

			// Remove events from cassandra cluster.
			for (String idToDelete : this.idsToDelete) {
				UUID id = UUID.fromString(idToDelete);
				Row row = this.session.execute(this.eventSelectionStatement.bind(id)).one();
				if (row == null) {
					continue;
				}
				if (!this.etmConfiguration.isDataRetentionPreserveEventPerformances()) {
					// TODO remove from performance tables.
				}
				if (!this.etmConfiguration.isDataRetentionPreserveEventCounts()) {
					// TODO remove from count tables.
				}
				if (!this.etmConfiguration.isDataRetentionPreserveEventCounts() && this.etmConfiguration.isDataRetentionPreserveEventPerformances()) {
					// TODO remove form event_occurrenecs table.
				}
				// TODO remove from other tables.
			}
			this.idsToDelete.clear();
        } catch (SolrServerException | IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Error removing events from system.", e);
        	}
        }
    }

	@Override
	public void streamDocListInfo(long numFound, long start, Float maxScore) {
	}

	@Override
    public void close() throws IOException {
	    if (this.idsToDelete.size() != 0) {
	    	removeEvents();
	    }
    }

}
