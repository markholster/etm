package com.jecstar.etm.gui.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsAction;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService.ScriptType;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.EtmPrincipal;
import com.jecstar.etm.core.domain.converter.json.AbstractJsonConverter;

@Path("/search")
public class SearchService extends AbstractJsonConverter {

	private static Client client;
	private static EtmConfiguration etmConfiguraton;
	
    @Context
    private SecurityContext securityContext;
	
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
		
		scriptParams.put("template", template);
		SearchService.client.prepareUpdate("etm_configuration", "user", ((EtmPrincipal)this.securityContext.getUserPrincipal()).getId())
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
		SearchService.client.prepareUpdate("etm_configuration", "user", ((EtmPrincipal)this.securityContext.getUserPrincipal()).getId())
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
		List<String> names = new ArrayList<String>();
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
				// TODO check op meegegeven type.
				MappingMetaData mappingMetaData = mappingMetadataCursor.value;
				try {
					Map<String, Object> valueMap = mappingMetaData.getSourceAsMap();
					addProperties(names, "", valueMap);
				} catch (IOException e) {
					// TODO logging.
				}
			}
			
		}
		Collections.sort(names);
		names.add("_exists_");
		names.add("_missing_");
		result.append("{ \"keywords\":[");
		result.append(names.stream().map(n -> "\"" + escapeToJson(n)+ "\"").collect(Collectors.joining(", ")));
		result.append("]}");
		return result.toString();
	}
	
	@POST
	@Path("/query")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String executeQuery(String json) {
		Map<String, Object> requestValues = toMap(json);
		String query = getString("query", requestValues);
		long startTime = System.currentTimeMillis();
		SearchResponse response = client.prepareSearch("etm_event_all")
			.setQuery(new QueryStringQueryBuilder(query)
					.allowLeadingWildcard(true)
					.analyzeWildcard(true)
					.defaultField("payload")
					.locale(((EtmPrincipal)this.securityContext.getUserPrincipal()).getLocale())
					.lowercaseExpandedTerms(false)
					.timeZone(((EtmPrincipal)this.securityContext.getUserPrincipal()).getTimeZone().getID()))
			.get();
		long endTime = System.currentTimeMillis();
		StringBuilder result = new StringBuilder();
		result.append("{");
		result.append("\"status\": \"success\"");
		result.append(",\"query-time\": " + (endTime - startTime));
		result.append(",\"hits\": " + response.getHits().getTotalHits());
		result.append("}");
		return result.toString();
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
