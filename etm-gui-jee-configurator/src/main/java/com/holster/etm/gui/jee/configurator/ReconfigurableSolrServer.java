package com.holster.etm.gui.jee.configurator;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.StreamingResponseCallback;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

public class ReconfigurableSolrServer extends SolrServer {

	/**
	 * The serialVersionUID for this class.
	 */
    private static final long serialVersionUID = -6339144110775263972L;
    
    
	private CloudSolrServer solrServer;

	public ReconfigurableSolrServer(CloudSolrServer cloudSolrServer) {
		this.solrServer = cloudSolrServer;
	}

	@Override
    public UpdateResponse add(Collection<SolrInputDocument> docs) throws SolrServerException, IOException {
	    return this.solrServer.add(docs);
    }

	@Override
    public UpdateResponse add(Collection<SolrInputDocument> docs, int commitWithinMs) throws SolrServerException, IOException {
	    return this.solrServer.add(docs, commitWithinMs);
    }

	@Override
    public UpdateResponse addBeans(Collection<?> beans) throws SolrServerException, IOException {
	    return this.solrServer.addBeans(beans);
    }

	@Override
    public UpdateResponse addBeans(Collection<?> beans, int commitWithinMs) throws SolrServerException, IOException {
	    return this.solrServer.addBeans(beans, commitWithinMs);
    }

	@Override
    public UpdateResponse add(SolrInputDocument doc) throws SolrServerException, IOException {
	    return this.solrServer.add(doc);
    }

	@Override
    public UpdateResponse add(SolrInputDocument doc, int commitWithinMs) throws SolrServerException, IOException {
	    return this.solrServer.add(doc, commitWithinMs);
    }

	@Override
    public UpdateResponse addBean(Object obj) throws IOException, SolrServerException {
	    return this.solrServer.addBean(obj);
    }

	@Override
    public UpdateResponse addBean(Object obj, int commitWithinMs) throws IOException, SolrServerException {
	    return this.solrServer.addBean(obj, commitWithinMs);
    }

	@Override
    public UpdateResponse commit() throws SolrServerException, IOException {
	    return this.solrServer.commit();
    }

	@Override
    public UpdateResponse optimize() throws SolrServerException, IOException {
	    return this.solrServer.optimize();
    }

	@Override
    public UpdateResponse commit(boolean waitFlush, boolean waitSearcher) throws SolrServerException, IOException {
	    return this.solrServer.commit(waitFlush, waitSearcher);
    }

	@Override
    public UpdateResponse commit(boolean waitFlush, boolean waitSearcher, boolean softCommit) throws SolrServerException, IOException {
	    return this.solrServer.commit(waitFlush, waitSearcher, softCommit);
    }

	@Override
    public UpdateResponse optimize(boolean waitFlush, boolean waitSearcher) throws SolrServerException, IOException {
	    return this.solrServer.optimize(waitFlush, waitSearcher);
    }

	@Override
    public UpdateResponse optimize(boolean waitFlush, boolean waitSearcher, int maxSegments) throws SolrServerException, IOException {
	    return this.solrServer.optimize(waitFlush, waitSearcher, maxSegments);
    }

	@Override
    public UpdateResponse rollback() throws SolrServerException, IOException {
	    return this.solrServer.rollback();
    }

	@Override
    public UpdateResponse deleteById(String id) throws SolrServerException, IOException {
	    return this.solrServer.deleteById(id);
    }

	@Override
    public UpdateResponse deleteById(String id, int commitWithinMs) throws SolrServerException, IOException {
	    return this.solrServer.deleteById(id, commitWithinMs);
    }

	@Override
    public UpdateResponse deleteById(List<String> ids) throws SolrServerException, IOException {
	    return this.solrServer.deleteById(ids);
    }

	@Override
    public UpdateResponse deleteById(List<String> ids, int commitWithinMs) throws SolrServerException, IOException {
	    return this.solrServer.deleteById(ids, commitWithinMs);
    }

	@Override
    public UpdateResponse deleteByQuery(String query) throws SolrServerException, IOException {
	    return this.solrServer.deleteByQuery(query);
    }

	@Override
    public UpdateResponse deleteByQuery(String query, int commitWithinMs) throws SolrServerException, IOException {
	    return this.solrServer.deleteByQuery(query, commitWithinMs);
    }

	@Override
    public SolrPingResponse ping() throws SolrServerException, IOException {
	    return this.solrServer.ping();
    }

	@Override
    public QueryResponse query(SolrParams params) throws SolrServerException {
	    return this.solrServer.query(params);
    }

	@Override
    public QueryResponse query(SolrParams params, METHOD method) throws SolrServerException {
	    return this.solrServer.query(params, method);
    }

	@Override
    public QueryResponse queryAndStreamResponse(SolrParams params, StreamingResponseCallback callback) throws SolrServerException,
            IOException {
	    return this.solrServer.queryAndStreamResponse(params, callback);
    }

	@Override
    public DocumentObjectBinder getBinder() {
	    return this.solrServer.getBinder();
    }

	@Override
    public NamedList<Object> request(SolrRequest request) throws SolrServerException, IOException {
	    return this.solrServer.request(request);
    }

	@Override
    public void shutdown() {
		this.solrServer.shutdown();
    }
	
	public void switchToCloudSolrServer(CloudSolrServer newCloudSolrServer) {
		CloudSolrServer oldCloudSolrServer = this.solrServer;
		this.solrServer = newCloudSolrServer;
		oldCloudSolrServer.shutdown();
    }

	public void setDefaultCollection(String solrCollectionName) {
	    this.solrServer.setDefaultCollection(solrCollectionName);
    }

}
