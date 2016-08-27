package com.jecstar.etm.gui.rest.services.iib;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.jecstar.etm.gui.rest.AbstractJsonService;
import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

@Path("/iib")
public class IIBService extends AbstractJsonService {

	private static Client client;
	private static EtmConfiguration etmConfiguration;
	
	public static void initialize(Client client, EtmConfiguration etmConfiguration) {
		IIBService.client = client;
		IIBService.etmConfiguration = etmConfiguration;
	}
	
	
	@GET
	@Path("/nodes")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getParsers() {
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticSearchLayout.CONFIGURATION_INDEX_NAME)
			.setTypes(ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_IIB_NODE)
			.setFetchSource(true)
			.setQuery(QueryBuilders.matchAllQuery())
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
		if (!scrollableSearch.hasNext()) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		result.append("{\"nodes\": [");
		boolean first = true;
		for (SearchHit searchHit : scrollableSearch) {
			if (!first) {
				result.append(",");
			}
			result.append(searchHit.getSourceAsString());
			first = false;
		}
		result.append("]}");
		return result.toString();
	}
	
	@DELETE
	@Path("/node/{nodeName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String deleteParser(@PathParam("nodeName") String nodeName) {
		client.prepareDelete(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_IIB_NODE, nodeName)
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.get();
		return "{\"status\":\"success\"}";
	}
	
	@PUT
	@Path("/node/{nodeName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String addParser(@PathParam("nodeName") String nodeName, String json) {
		// TODO try to setup an connection?
		client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_IIB_NODE, nodeName)
			.setDoc("")
			.setDocAsUpsert(true)
			.setDetectNoop(true)
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
			.get();
		return "{ \"status\": \"success\" }";
	}
}
