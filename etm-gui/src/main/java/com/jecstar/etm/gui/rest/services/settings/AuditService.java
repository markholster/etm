package com.jecstar.etm.gui.rest.services.settings;

import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTimeZone;

import com.jecstar.etm.gui.rest.services.AbstractIndexMetadataService;
import com.jecstar.etm.gui.rest.services.Keyword;
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.EtmPrincipal;

@Path("/audit")
public class AuditService extends AbstractIndexMetadataService {
	

	private static Client client;
	private static EtmConfiguration etmConfiguration;
	
	public static void initialize(Client client, EtmConfiguration etmConfiguration) {
		AuditService.client = client;
		AuditService.etmConfiguration = etmConfiguration;
	}

	@GET
	@Path("/keywords/{indexName}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getKeywords(@PathParam("indexName") String indexName) {
		StringBuilder result = new StringBuilder();
		Map<String, List<Keyword>> names = getIndexFields(AuditService.client, indexName);
		result.append("{ \"keywords\":[");
		Set<Entry<String, List<Keyword>>> entries = names.entrySet();
		if (entries != null) {
			boolean first = true;
			for (Entry<String, List<Keyword>> entry : entries) {
				if (!first) {
					result.append(", ");
				}
				first = false;
				result.append("{");
				result.append("\"index\": " + escapeToJson(indexName, true) + ",");
				result.append("\"type\": " + escapeToJson(entry.getKey(), true) + ",");
				result.append("\"keywords\": [" + entry.getValue().stream().map(n -> {
					StringBuilder kw = new StringBuilder();
					kw.append("{");
					addStringElementToJsonBuffer("name", n.getName(), kw, true);
					addStringElementToJsonBuffer("type", n.getType(), kw, false);
					addBooleanElementToJsonBuffer("date", n.isDate(), kw, false);
					addBooleanElementToJsonBuffer("number", n.isNumber(), kw, false);
					kw.append("}");
					return kw.toString();
				}).collect(Collectors.joining(", ")) + "]");
				result.append("}");
			}
		}
		result.append("]}");
		return result.toString();
	}
	
	@POST
	@Path("/query")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String executeQuery(String json) {
		long startTime = System.currentTimeMillis();
		EtmPrincipal etmPrincipal = getEtmPrincipal(); 
		
		AuditSearchRequestParameters parameters = new AuditSearchRequestParameters(toMap(json));
		SearchRequestBuilder requestBuilder = createRequestFromInput(parameters, etmPrincipal);
		NumberFormat numberFormat = NumberFormat.getInstance(etmPrincipal.getLocale());
		SearchResponse response = requestBuilder.get();
		StringBuilder result = new StringBuilder();
		result.append("{");
		result.append("\"status\": \"success\"");
		result.append(",\"hits\": " + response.getHits().getTotalHits());
		result.append(",\"hits_as_string\": \"" + numberFormat.format(response.getHits().getTotalHits()) + "\"");
		result.append(",\"time_zone\": \"" + etmPrincipal.getTimeZone().getID() + "\"");
		result.append(",\"start_ix\": " + parameters.getStartIndex());
		result.append(",\"end_ix\": " + (parameters.getStartIndex() + response.getHits().hits().length - 1));
		result.append(",\"has_more_results\": " + (parameters.getStartIndex() + response.getHits().hits().length < response.getHits().getTotalHits() - 1));
		result.append(",\"time_zone\": \"" + etmPrincipal.getTimeZone().getID() + "\"");
		result.append(",\"results\": [");
		addSearchHits(result, response.getHits());
		result.append("]");
		long queryTime = System.currentTimeMillis() - startTime;
		result.append(",\"query_time\": " + queryTime);
		result.append(",\"query_time_as_string\": \"" + numberFormat.format(queryTime) + "\"");
		result.append("}");		
		return result.toString();
	}
	
	private SearchRequestBuilder createRequestFromInput(AuditSearchRequestParameters parameters, EtmPrincipal etmPrincipal) {
		QueryStringQueryBuilder queryStringBuilder = new QueryStringQueryBuilder(parameters.getQueryString())
			.allowLeadingWildcard(true)
			.analyzeWildcard(true)
			.defaultField("_all")
			.timeZone(DateTimeZone.forTimeZone(etmPrincipal.getTimeZone()));
		BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
		boolQueryBuilder.must(queryStringBuilder);
		boolQueryBuilder.filter(new RangeQueryBuilder("timestamp").lte(parameters.getNotAfterTimestamp()));
		SearchRequestBuilder requestBuilder = client.prepareSearch(ElasticSearchLayout.ETM_AUDIT_LOG_INDEX_ALIAS_ALL)
			.setQuery(boolQueryBuilder)
			.setFetchSource(true)
			.setFrom(parameters.getStartIndex())
			.setSize(parameters.getMaxResults() > 500 ? 500 : parameters.getMaxResults())
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		if (parameters.getSortField() != null && parameters.getSortField().trim().length() > 0) {
			requestBuilder.addSort(parameters.getSortField(), "desc".equals(parameters.getSortOrder()) ? SortOrder.DESC : SortOrder.ASC);
		}
		return requestBuilder;
	}
	
	private void addSearchHits(StringBuilder result, SearchHits hits) {
		boolean first = true;
		for (SearchHit searchHit : hits.getHits()) {
			if (first) {
				result.append("{");
				first = false;
			} else {
				result.append(", {");
			}
			addStringElementToJsonBuffer("index", searchHit.getIndex() , result, true);
			addStringElementToJsonBuffer("type", searchHit.getType() , result, false);
			addStringElementToJsonBuffer("id", searchHit.getId() , result, false);
			result.append(", \"source\": " + searchHit.getSourceAsString());
			result.append("}");
		}
	}

}
