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
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.EtmPrincipal;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverterTags;
import com.jecstar.etm.core.domain.converter.json.TelemetryEventConverterTagsJsonImpl;

@Path("/search")
public class SearchService extends AbstractJsonService {

	private static Client client;
	private static EtmConfiguration etmConfiguraton;
	
	private final TelemetryEventConverterTags tags = new TelemetryEventConverterTagsJsonImpl();
	
	public static void initialize(Client client, EtmConfiguration etmConfiguration) {
		SearchService.client = client;
		SearchService.etmConfiguraton = etmConfiguration;
	}
	
	@GET
	@Path("/templates")
	@Produces(MediaType.APPLICATION_JSON)
	public String getSearchTemplates() {
		GetResponse getResponse = SearchService.client.prepareGet("etm_configuration", "user", ((EtmPrincipal)this.securityContext.getUserPrincipal()).getId())
				.setFetchSource("searchtemplates", null)
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
		
		scriptParams.put("template", template);
		SearchService.client.prepareUpdate("etm_configuration", "user", getEtmPrincipal().getId())
				.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguraton.getWriteConsistency().name()))
				.setScript(new Script("etm_update-searchtemplate", ScriptType.FILE, "groovy", scriptParams))
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
			.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguraton.getWriteConsistency().name()))
			.setScript(new Script("etm_remove-searchtemplate", ScriptType.FILE, "groovy", scriptParams))
			.setRetryOnConflict(3)
			.get();
		return "{ \"status\": \"success\" }";
	}
	
	@GET
	@Path("/keywords")
	@Produces(MediaType.APPLICATION_JSON)
	public String getKeywords() {
		StringBuilder result = new StringBuilder();
		Map<String, List<String>> names = new HashMap<String, List<String>>();
		GetMappingsResponse mappingsResponse = new GetMappingsRequestBuilder(SearchService.client, GetMappingsAction.INSTANCE, "etm_event_all").get();
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
					names.put(mappingMetadataCursor.key, values);
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
				result.append("{");
				result.append("\"type\": \"" + escapeToJson(entry.getKey()) + "\",");
				result.append("\"names\": [" + entry.getValue().stream().map(n -> "\"" + escapeToJson(n)+ "\"").collect(Collectors.joining(", ")) + "]");
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
		if (fields == null) {
			fields = new ArrayList<String>(1);
		}
		if (fields.isEmpty()) {
			fields.add(this.tags.getNameTag());
			fields.add(this.tags.getEndpointsTag() + "." + this.tags.getWritingEndpointHandlerTag() + "." + this.tags.getEndpointHandlerHandlingTimeTag());
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
		if (sortField != null) {
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
		result.append(",\"hits\": " + response.getHits().getTotalHits());
		result.append(",\"hits_as_string\": \"" + numberFormat.format(response.getHits().getTotalHits()) + "\"");
		result.append(",\"results\": [");
		addSearchHits(result, response.getHits());
		result.append("]");
		long queryTime = System.currentTimeMillis() - startTime;
		result.append(",\"query_time\": " + queryTime);
		result.append(",\"query_time_as_string\": \"" + numberFormat.format(queryTime) + "\"");
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
			boolean added = false;
			added = addStringElementToJsonBuffer("type", searchHit.getType() , result, !added) || added;
			added = addStringElementToJsonBuffer("id", searchHit.getId() , result, !added) || added;
			added = addStringElementToJsonBuffer("index", searchHit.getIndex() , result, !added) || added;
			result.append(", \"source\": ");
			result.append(searchHit.getSourceAsString());
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
