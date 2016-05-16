package com.jecstar.etm.gui.rest;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsAction;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.jecstar.etm.domain.writers.TelemetryEventTags;
import com.jecstar.etm.domain.writers.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.EtmPrincipal;

@Path("/search")
public class SearchService extends AbstractJsonService {

	private static Client client;
	private static EtmConfiguration etmConfiguration;
	
	private final TelemetryEventTags tags = new TelemetryEventTagsJsonImpl();
	
	public static void initialize(Client client, EtmConfiguration etmConfiguration) {
		SearchService.client = client;
		SearchService.etmConfiguration = etmConfiguration;
	}
	
	@GET
	@Path("/templates")
	@Produces(MediaType.APPLICATION_JSON)
	public String getSearchTemplates() {
		GetResponse getResponse = SearchService.client.prepareGet("etm_configuration", "user", getEtmPrincipal().getId())
				.setFetchSource("search_templates", null)
				.get();
		if (getResponse.isSourceEmpty()) {
			return "{}";
		}
		return getResponse.getSourceAsString();
	}
	
	@GET
	@Path("/query_history")
	@Produces(MediaType.APPLICATION_JSON)
	public String getRecentQueries() {
		EtmPrincipal etmPrincipal = getEtmPrincipal();
		GetResponse getResponse = SearchService.client.prepareGet("etm_configuration", "user", etmPrincipal.getId())
				.setFetchSource("query_history", null)
				.get();
		if (getResponse.isSourceEmpty()) {
			return "{}";
		}
		return getResponse.getSourceAsString();
	}
	
	@PUT
	@Path("/templates/{templateName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String addSearchTemplate(@PathParam("templateName") String templateName, String json) {
		Map<String, Object> requestValues = toMap(json); 
		Map<String, Object> scriptParams = new HashMap<String, Object>();
		Map<String, Object> template = new HashMap<String, Object>();
		template.put("name", templateName);
		template.put("query", getString("query", requestValues));
		template.put("types", getArray("types", requestValues));
		template.put("fields", getArray("fields", requestValues));
		template.put("results_per_page", getInteger("results_per_page", requestValues, 50));
		template.put("sort_field", getString("sort_field", requestValues));
		template.put("sort_order", getString("sort_order", requestValues));
		
		scriptParams.put("template", template);
		SearchService.client.prepareUpdate("etm_configuration", "user", getEtmPrincipal().getId())
				.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
				.setScript(new Script("etm_update-search-template", ScriptType.STORED, "painless", scriptParams))
				.setRetryOnConflict(3)
				.get();
		return "{ \"status\": \"success\" }";
	}

	@DELETE
	@Path("/templates/{templateName}")
	@Produces(MediaType.APPLICATION_JSON)
	public String removeSearchTemplate(@PathParam("templateName") String templateName) {
		Map<String, Object> scriptParams = new HashMap<String, Object>();
		scriptParams.put("name", templateName);
		SearchService.client.prepareUpdate("etm_configuration", "user", getEtmPrincipal().getId())
			.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
			.setScript(new Script("etm_remove-search-template", ScriptType.STORED, "painless", scriptParams))
			.setRetryOnConflict(3)
			.get();
		return "{ \"status\": \"success\" }";
	}
	
	@GET
	@Path("/keywords/{indexName}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getKeywords(@PathParam("indexName") String indexName) {
		StringBuilder result = new StringBuilder();
		Map<String, List<String>> names = new HashMap<String, List<String>>();
		GetMappingsResponse mappingsResponse = new GetMappingsRequestBuilder(SearchService.client, GetMappingsAction.INSTANCE, indexName).get();
		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = mappingsResponse.getMappings();
		Iterator<ObjectObjectCursor<String, ImmutableOpenMap<String, MappingMetaData>>> mappingsIterator = mappings.iterator();
		while (mappingsIterator.hasNext()) {
			ObjectObjectCursor<String, ImmutableOpenMap<String, MappingMetaData>> mappingsCursor = mappingsIterator.next();
			Iterator<ObjectObjectCursor<String, MappingMetaData>> mappingMetaDataIterator = mappingsCursor.value.iterator();
			while (mappingMetaDataIterator.hasNext()) {
				ObjectObjectCursor<String, MappingMetaData> mappingMetadataCursor = mappingMetaDataIterator.next();
				if ("_default_".equals(mappingMetadataCursor.key)) {
					continue;
				}
				MappingMetaData mappingMetaData = mappingMetadataCursor.value;
				List<String> values = new ArrayList<String>();
				values.add("_exists_");
				values.add("_missing_");
				try {
					Map<String, Object> valueMap = mappingMetaData.getSourceAsMap();
					addProperties(values, "", valueMap);
					if (names.containsKey(mappingMetadataCursor.key)) {
						List<String> currentValues = names.get(mappingMetadataCursor.key);
						for (String value : values) {
							if (!currentValues.contains(value)) {
								currentValues.add(value);
							}
						}
					} else {
						names.put(mappingMetadataCursor.key, values);
					}
				} catch (IOException e) {
					// TODO logging.
				}
			}
			
		}
		result.append("{ \"keywords\":[");
		Set<Entry<String, List<String>>> entries = names.entrySet();
		if (entries != null) {
			boolean first = true;
			for (Entry<String, List<String>> entry : entries) {
				if (!first) {
					result.append(", ");
				}
				first = false;
				result.append("{");
				result.append("\"type\": " + escapeToJson(entry.getKey(), true) + ",");
				result.append("\"names\": [" + entry.getValue().stream().map(n -> escapeToJson(n, true)).collect(Collectors.joining(", ")) + "]");
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
		int history_size = 5;
		Map<String, Object> requestValues = toMap(json);
		String queryString = getString("query", requestValues);
		Integer startIndex = getInteger("start_ix", requestValues, new Integer(0));
		Integer maxResults = getInteger("max_results", requestValues, new Integer(50));
		if (maxResults > 500) {
			maxResults = 50;
		}
		String sortField = getString("sort_field", requestValues);
		String sortOrder = getString("sort_order", requestValues);
		List<String> types = getArray("types", requestValues);
		List<String> fields = getArray("fields", requestValues);
		List<Map<String, Object>> fieldsLayout = getArray("fieldsLayout", requestValues);
		if (fields == null) {
			fields = new ArrayList<String>(2);
		}
		if (fields.isEmpty()) {
			fields.add(this.tags.getEndpointsTag() + "." + this.tags.getWritingEndpointHandlerTag() + "." + this.tags.getEndpointHandlerHandlingTimeTag());
			fields.add(this.tags.getNameTag());
		}
		EtmPrincipal etmPrincipal = getEtmPrincipal(); 
		QueryStringQueryBuilder queryStringBuilder = new QueryStringQueryBuilder(queryString)
			.allowLeadingWildcard(true)
			.analyzeWildcard(true)
			.defaultField("payload")
			.locale(etmPrincipal.getLocale())
			.lowercaseExpandedTerms(false)
			.timeZone(etmPrincipal.getTimeZone().getID());
		SearchRequestBuilder requestBuilder = client.prepareSearch("etm_event_all")
			.setQuery(addEtmPrincipalFilterQuery(queryStringBuilder))
			.setFetchSource(fields.toArray(new String[fields.size()]), null)
			.setFrom(startIndex)
			.setSize(maxResults);
		if (sortField != null && sortField.trim().length() > 0) {
			requestBuilder.addSort(sortField, "desc".equals(sortOrder) ? SortOrder.DESC : SortOrder.ASC);
		}
		if (types != null && !types.isEmpty()) {
			requestBuilder.setTypes(types.toArray(new String[types.size()]));
		}
		NumberFormat numberFormat = NumberFormat.getInstance(etmPrincipal.getLocale());
		SearchResponse response = requestBuilder.get();
		StringBuilder result = new StringBuilder();
		result.append("{");
		result.append("\"status\": \"success\"");
		result.append(",\"history_size\": " + history_size);
		result.append(",\"hits\": " + response.getHits().getTotalHits());
		result.append(",\"hits_as_string\": \"" + numberFormat.format(response.getHits().getTotalHits()) + "\"");
		result.append(",\"time_zone\": \"" + etmPrincipal.getTimeZone().getID() + "\"");
		result.append(",\"start_ix\": " + startIndex);
		result.append(",\"end_ix\": " + (startIndex + response.getHits().hits().length - 1));
		result.append(",\"has_more_results\": " + (startIndex + response.getHits().hits().length < response.getHits().getTotalHits() - 1));
		result.append(",\"time_zone\": \"" + etmPrincipal.getTimeZone().getID() + "\"");
		result.append(",\"results\": [");
		addSearchHits(result, response.getHits());
		result.append("]");
		long queryTime = System.currentTimeMillis() - startTime;
		result.append(",\"query_time\": " + queryTime);
		result.append(",\"query_time_as_string\": \"" + numberFormat.format(queryTime) + "\"");
		result.append("}");
		
		if (startIndex == 0) {
			writeQueryHistory(startTime, queryString, startIndex, maxResults, sortField, sortOrder, types, fieldsLayout, etmPrincipal, history_size);
		}
		return result.toString();
	}
	
	private void writeQueryHistory(long timestamp, String queryString, Integer startIndex, Integer maxResults,
			String sortField, String sortOrder, List<String> types, List<Map<String, Object>> fieldsLayout, EtmPrincipal etmPrincipal, int history_size) {
		Map<String, Object> scriptParams = new HashMap<String, Object>();
		Map<String, Object> query = new HashMap<String, Object>();
		query.put("timestamp", timestamp);
		query.put("query", queryString);
		query.put("types", types);
		query.put("fields", fieldsLayout);
		query.put("results_per_page", maxResults);
		query.put("sort_field", sortField);
		query.put("sort_order", sortOrder);
		
		scriptParams.put("query", query);
		scriptParams.put("history_size", history_size);
		SearchService.client.prepareUpdate("etm_configuration", "user", getEtmPrincipal().getId())
				.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
				.setScript(new Script("etm_update-query-history", ScriptType.STORED, "painless", scriptParams))
				.setRetryOnConflict(3)
				.execute();
	}

	@GET
	@Path("/event/{index}/{type}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getEvent(@PathParam("index") String index, @PathParam("type") String type, @PathParam("id") String id) {
		GetResponse getResponse = SearchService.client.prepareGet(index, type, id).get();
		if (getResponse.isSourceEmpty()) {
			return "{}";
		}
		StringBuilder result = new StringBuilder();
		result.append("{");
		addStringElementToJsonBuffer("index", getResponse.getIndex() , result, true);
		addStringElementToJsonBuffer("type", getResponse.getType() , result, false);
		addStringElementToJsonBuffer("id", getResponse.getId() , result, false);
		addStringElementToJsonBuffer("time_zone", getEtmPrincipal().getTimeZone().getID() , result, false);
		result.append(", \"source\": " + getResponse.getSourceAsString());
		result.append("}");
		return result.toString();
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
			addStringElementToJsonBuffer("type", searchHit.getType() , result, true);
			addStringElementToJsonBuffer("id", searchHit.getId() , result, false);
			addStringElementToJsonBuffer("index", searchHit.getIndex() , result, false);
			result.append(", \"source\": " + searchHit.getSourceAsString());
			result.append("}");
		}
	}

	@SuppressWarnings("unchecked")
	private void addProperties(List<String> names, String prefix, Map<String, Object> valueMap) {
		valueMap = getObject("properties", valueMap);
		if (valueMap == null) {
			return;
		}
		for (Entry<String, Object> entry : valueMap.entrySet()) {
			Map<String, Object> entryValues = (Map<String, Object>) entry.getValue();
			String name = determineName(prefix, entry.getKey());
			if (entryValues.containsKey("properties")) {
				addProperties(names, name, entryValues);
			} else {
				if (!names.contains(name)) {
					names.add(name);
				}
			}
		}
	}
	
	private String determineName(String prefix, String name) {
		if (prefix.length() == 0) {
			return name;
		}
		return prefix + "." + name;
	}

}
