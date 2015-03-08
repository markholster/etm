package com.jecstar.etm.gui.jee.configurator;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.StreamingResponseCallback;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient.RouteResponse;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

public class ReconfigurableSolrClient extends SolrClient {

	/**
	 * The serialVersionUID for this class.
	 */
    private static final long serialVersionUID = -6339144110775263972L;
    
    
	private CloudSolrClient solrClient;

	public UpdateResponse add(Collection<SolrInputDocument> docs) throws SolrServerException, IOException {
	    return this.solrClient.add(docs);
    }

	public UpdateResponse add(Collection<SolrInputDocument> docs, int commitWithinMs) throws SolrServerException, IOException {
	    return this.solrClient.add(docs, commitWithinMs);
    }

	public int hashCode() {
	    return this.solrClient.hashCode();
    }

	public UpdateResponse addBeans(Collection<?> beans) throws SolrServerException, IOException {
	    return this.solrClient.addBeans(beans);
    }

	public UpdateResponse addBeans(Collection<?> beans, int commitWithinMs) throws SolrServerException, IOException {
	    return this.solrClient.addBeans(beans, commitWithinMs);
    }

	public UpdateResponse add(SolrInputDocument doc) throws SolrServerException, IOException {
	    return this.solrClient.add(doc);
    }

	public UpdateResponse add(SolrInputDocument doc, int commitWithinMs) throws SolrServerException, IOException {
	    return this.solrClient.add(doc, commitWithinMs);
    }

	public boolean equals(Object obj) {
	    return this.solrClient.equals(obj);
    }

	public UpdateResponse addBean(Object obj) throws IOException, SolrServerException {
	    return this.solrClient.addBean(obj);
    }

	public UpdateResponse addBean(Object obj, int commitWithinMs) throws IOException, SolrServerException {
	    return this.solrClient.addBean(obj, commitWithinMs);
    }

	public UpdateResponse commit() throws SolrServerException, IOException {
	    return this.solrClient.commit();
    }

	public UpdateResponse optimize() throws SolrServerException, IOException {
	    return this.solrClient.optimize();
    }

	public UpdateResponse commit(boolean waitFlush, boolean waitSearcher) throws SolrServerException, IOException {
	    return this.solrClient.commit(waitFlush, waitSearcher);
    }

	public UpdateResponse commit(boolean waitFlush, boolean waitSearcher, boolean softCommit) throws SolrServerException, IOException {
	    return this.solrClient.commit(waitFlush, waitSearcher, softCommit);
    }

	public UpdateResponse optimize(boolean waitFlush, boolean waitSearcher) throws SolrServerException, IOException {
	    return this.solrClient.optimize(waitFlush, waitSearcher);
    }

	public UpdateResponse optimize(boolean waitFlush, boolean waitSearcher, int maxSegments) throws SolrServerException, IOException {
	    return this.solrClient.optimize(waitFlush, waitSearcher, maxSegments);
    }

	public UpdateResponse rollback() throws SolrServerException, IOException {
	    return this.solrClient.rollback();
    }

	public UpdateResponse deleteById(String id) throws SolrServerException, IOException {
	    return this.solrClient.deleteById(id);
    }

	public UpdateResponse deleteById(String id, int commitWithinMs) throws SolrServerException, IOException {
	    return this.solrClient.deleteById(id, commitWithinMs);
    }

	public UpdateResponse deleteById(List<String> ids) throws SolrServerException, IOException {
	    return this.solrClient.deleteById(ids);
    }

	public String toString() {
	    return this.solrClient.toString();
    }

	public UpdateResponse deleteById(List<String> ids, int commitWithinMs) throws SolrServerException, IOException {
	    return this.solrClient.deleteById(ids, commitWithinMs);
    }

	public UpdateResponse deleteByQuery(String query) throws SolrServerException, IOException {
	    return this.solrClient.deleteByQuery(query);
    }

	public UpdateResponse deleteByQuery(String query, int commitWithinMs) throws SolrServerException, IOException {
	    return this.solrClient.deleteByQuery(query, commitWithinMs);
    }

	public SolrPingResponse ping() throws SolrServerException, IOException {
	    return this.solrClient.ping();
    }

	public QueryResponse query(SolrParams params) throws SolrServerException {
	    return this.solrClient.query(params);
    }

	public QueryResponse query(SolrParams params, METHOD method) throws SolrServerException {
	    return this.solrClient.query(params, method);
    }

	public QueryResponse queryAndStreamResponse(SolrParams params, StreamingResponseCallback callback) throws SolrServerException,
            IOException {
	    return this.solrClient.queryAndStreamResponse(params, callback);
    }

	public DocumentObjectBinder getBinder() {
	    return this.solrClient.getBinder();
    }

	public void setCollectionCacheTTl(int seconds) {
		this.solrClient.setCollectionCacheTTl(seconds);
    }

	public ResponseParser getParser() {
	    return this.solrClient.getParser();
    }

	public void setParser(ResponseParser processor) {
		this.solrClient.setParser(processor);
    }

	public RequestWriter getRequestWriter() {
	    return this.solrClient.getRequestWriter();
    }

	public void setRequestWriter(RequestWriter requestWriter) {
		this.solrClient.setRequestWriter(requestWriter);
    }

	public String getZkHost() {
	    return this.solrClient.getZkHost();
    }

	public ZkStateReader getZkStateReader() {
	    return this.solrClient.getZkStateReader();
    }

	public void setIdField(String idField) {
		this.solrClient.setIdField(idField);
    }

	public String getIdField() {
	    return this.solrClient.getIdField();
    }

	public String getDefaultCollection() {
	    return this.solrClient.getDefaultCollection();
    }

	public void setZkConnectTimeout(int zkConnectTimeout) {
		this.solrClient.setZkConnectTimeout(zkConnectTimeout);
    }

	public void setZkClientTimeout(int zkClientTimeout) {
		this.solrClient.setZkClientTimeout(zkClientTimeout);
    }

	public void connect() {
		this.solrClient.connect();
    }

	public void setParallelUpdates(boolean parallelUpdates) {
		this.solrClient.setParallelUpdates(parallelUpdates);
    }

	public RouteResponse condenseResponse(NamedList<?> response, long timeMillis) {
	    return this.solrClient.condenseResponse(response, timeMillis);
    }

	public NamedList<Object> request(SolrRequest request) throws SolrServerException, IOException {
	    return this.solrClient.request(request);
    }

	public void shutdown() {
		this.solrClient.shutdown();
    }

	public LBHttpSolrClient getLbClient() {
	    return this.solrClient.getLbClient();
    }

	public boolean isUpdatesToLeaders() {
	    return this.solrClient.isUpdatesToLeaders();
    }

	public void setParallelCacheRefreshes(int n) {
		this.solrClient.setParallelCacheRefreshes(n);
    }

	public int getMinAchievedReplicationFactor(String collection, NamedList<?> resp) {
	    return this.solrClient.getMinAchievedReplicationFactor(collection, resp);
    }

	public Map<String, Integer> getShardReplicationFactor(String collection, NamedList<?> resp) {
	    return this.solrClient.getShardReplicationFactor(collection, resp);
    }

	public ReconfigurableSolrClient(CloudSolrClient cloudSolrClient) {
		this.solrClient = cloudSolrClient;
	}

	
	public void switchToCloudSolrClient(CloudSolrClient newCloudSolrClient) {
		SolrClient oldCloudSolrClient = this.solrClient;
		this.solrClient = newCloudSolrClient;
		oldCloudSolrClient.shutdown();
    }

	public void setDefaultCollection(String solrCollectionName) {
	    this.solrClient.setDefaultCollection(solrCollectionName);
    }

}
