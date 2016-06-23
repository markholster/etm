package com.jecstar.etm.gui.rest.services.search;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.elasticsearch.action.search.ClearScrollRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService.ScriptType;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.jecstar.etm.domain.HttpTelemetryEvent.HttpEventType;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.writers.TelemetryEventTags;
import com.jecstar.etm.domain.writers.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.gui.rest.AbstractJsonService;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.EtmPrincipal;

@Path("/search")
public class SearchService extends AbstractJsonService {

	private static Client client;
	private static EtmConfiguration etmConfiguration;
	
	private final TelemetryEventTags eventTags = new TelemetryEventTagsJsonImpl();
	
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
			fields.add(this.eventTags.getEndpointsTag() + "." + this.eventTags.getWritingEndpointHandlerTag() + "." + this.eventTags.getEndpointHandlerHandlingTimeTag());
			fields.add(this.eventTags.getNameTag());
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
			.setSize(maxResults)
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
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
		result.append(",\"history_size\": " + etmPrincipal.getHistorySize());
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
			writeQueryHistory(startTime, queryString, startIndex, maxResults, sortField, sortOrder, types, fieldsLayout, etmPrincipal, etmPrincipal.getHistorySize());
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
	@Path("/event/{type}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getEvent(@PathParam("type") String eventType, @PathParam("id") String eventId) {
		IdsQueryBuilder idsQueryBuilder = new IdsQueryBuilder(eventType)
				.addIds(eventId);
		SearchResponse response =  client.prepareSearch("etm_event_all")
			.setQuery(addEtmPrincipalFilterQuery(idsQueryBuilder))
			.setFetchSource(true)
			.setFrom(0)
			.setSize(1)
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.get();
		if (response.getHits().hits().length == 0) {
			return null;
		}
		SearchHit searchHit = response.getHits().getAt(0);
		StringBuilder result = new StringBuilder();
		result.append("{");
		addStringElementToJsonBuffer("index", searchHit.getIndex() , result, true);
		addStringElementToJsonBuffer("type", searchHit.getType() , result, false);
		addStringElementToJsonBuffer("id", searchHit.getId() , result, false);
		addStringElementToJsonBuffer("time_zone", getEtmPrincipal().getTimeZone().getID() , result, false);
		result.append(", \"source\": " + searchHit.getSourceAsString());
		result.append("}");
		return result.toString();
	}
	
	@SuppressWarnings("unchecked")
	@GET
	@Path("/transaction/{application}/{id}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getTransaction(@PathParam("application") String applicationName, @PathParam("id") String transactionId) {
		BoolQueryBuilder findEventsQuery = new BoolQueryBuilder()
			.minimumNumberShouldMatch(1)
			.should(
					new BoolQueryBuilder()
						.must(new TermQueryBuilder(this.eventTags.getEndpointsTag() + 
								"." + this.eventTags.getReadingEndpointHandlersTag() + 
								"." + this.eventTags.getEndpointHandlerApplicationTag() + 
								"." + this.eventTags.getApplicationNameTag(), applicationName))
						.must(new TermQueryBuilder(this.eventTags.getEndpointsTag() + 
								"." + this.eventTags.getReadingEndpointHandlersTag() + 
								"." + this.eventTags.getEndpointHandlerTransactionIdTag(), transactionId))
			).should(
					new BoolQueryBuilder()
						.must(new TermQueryBuilder(this.eventTags.getEndpointsTag() + 
								"." + this.eventTags.getWritingEndpointHandlerTag() + 
								"." + this.eventTags.getEndpointHandlerApplicationTag() + 
								"." + this.eventTags.getApplicationNameTag(), applicationName))
						.must(new TermQueryBuilder(this.eventTags.getEndpointsTag() + 
								"." + this.eventTags.getWritingEndpointHandlerTag() + 
								"." + this.eventTags.getEndpointHandlerTransactionIdTag(), transactionId))
		);
		final int scrollSize = 25;
		SearchResponse response =  client.prepareSearch("etm_event_all")
				.setQuery(addEtmPrincipalFilterQuery(findEventsQuery))
				.addSort(SortBuilders.fieldSort("_doc"))
				.setFetchSource(new String[] {
						this.eventTags.getEndpointsTag() + ".*", 
						this.eventTags.getNameTag(), 
						this.eventTags.getPayloadTag(),
						this.eventTags.getMessagingEventTypeTag(),
						this.eventTags.getHttpEventTypeTag(),
						this.eventTags.getSqlEventTypeTag()}, null)
				.setFrom(0)
				.setSize(scrollSize)
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
				.setScroll(new Scroll(TimeValue.timeValueSeconds(30)))
				.get();
		if (response.getHits().hits().length == 0) {
			return null;
		}
		List<TransactionEvent> events = new ArrayList<>();
		Set<String> scrollIds = new HashSet<>();
		StringBuilder result = new StringBuilder();
		String scrollId = response.getScrollId();
		scrollIds.add(scrollId);
		boolean nextBatchRequired = false;
		do {
			nextBatchRequired = scrollSize == response.getHits().hits().length;
			for (SearchHit searchHit : response.getHits().hits()) {
				TransactionEvent event = new TransactionEvent();
				event.index = searchHit.getIndex();
				event.type = searchHit.getType();
				event.id = searchHit.getId();
				Map<String, Object> source = searchHit.getSource();
				event.name = (String) source.get(this.eventTags.getNameTag());
				event.payload = (String) source.get(this.eventTags.getPayloadTag());
				List<Map<String, Object>> endpoints = (List<Map<String, Object>>) source.get(this.eventTags.getEndpointsTag());
				if (endpoints != null) {
					for (Map<String, Object> endpoint : endpoints) {
						Map<String, Object> writingEndpointHandler = (Map<String, Object>) endpoint.get(this.eventTags.getWritingEndpointHandlerTag());
						if (isWithinTransaction(writingEndpointHandler, applicationName, transactionId)) {
							event.handlingTime = (long) writingEndpointHandler.get(this.eventTags.getEndpointHandlerHandlingTimeTag());
							event.direction = "outgoing";
						} else {
							List<Map<String, Object>> readingEndpointHandlers = (List<Map<String, Object>>) endpoint.get(this.eventTags.getReadingEndpointHandlersTag());
							if (readingEndpointHandlers != null) {
								for (Map<String, Object> readingEndpointHandler : readingEndpointHandlers) {
									if (isWithinTransaction(readingEndpointHandler, applicationName, transactionId)) {
										event.handlingTime = (long) readingEndpointHandler.get(this.eventTags.getEndpointHandlerHandlingTimeTag());
										event.direction = "incomming";
									}
								}
							}
						}
						if ("http".equals(searchHit.getType())) {
							event.subType = (String) source.get(this.eventTags.getHttpEventTypeTag());
						} else if ("messaging".equals(searchHit.getType())) {
							event.subType = (String) source.get(this.eventTags.getMessagingEventTypeTag());
						} else if ("sql".equals(searchHit.getType())) {
							event.subType = (String) source.get(this.eventTags.getSqlEventTypeTag());
						}
					}
				}
				events.add(event);
			}
			if (nextBatchRequired) {
				// Full batch fetched, request the next batch.
				response = client.prepareSearchScroll(scrollId)
						.setScroll(TimeValue.timeValueSeconds(30))
						.get(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
				scrollId = response.getScrollId();
				scrollIds.add(scrollId);
			}
		} while (nextBatchRequired);
		clearScrolls(scrollIds);
		
		Collections.sort(events, (e1, e2) -> e1.handlingTime.compareTo(e2.handlingTime));
		result.append("{");
		addStringElementToJsonBuffer("time_zone", getEtmPrincipal().getTimeZone().getID() , result, true);
		result.append(",\"events\":[");
		boolean first = true;
		for (TransactionEvent event : events) {
			if (first) {
				result.append("{");
				first = false;
			} else {
				result.append(", {");
			}
			addStringElementToJsonBuffer("index", event.index , result, true);
			addStringElementToJsonBuffer("type", event.type , result, false);
			addStringElementToJsonBuffer("sub_type", event.subType , result, false);
			addStringElementToJsonBuffer("id", event.id , result, false);
			addLongElementToJsonBuffer("handling_time", event.handlingTime , result, false);
			addStringElementToJsonBuffer("name", event.name , result, false);
			addStringElementToJsonBuffer("direction", event.direction , result, false);
			addStringElementToJsonBuffer("payload", event.payload , result, false);
			result.append("}");
		}
		result.append("]}");
		return result.toString();
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("/event/{type}/{id}/chain")
	@Produces(MediaType.APPLICATION_JSON)
	public String getEventChain(@PathParam("type") String eventType, @PathParam("id") String eventId) {
		IdsQueryBuilder idsQueryBuilder = new IdsQueryBuilder(eventType)
				.addIds(eventId);
		// No principal filtered query. We would like to show the entire event chain, but the user should not be able to retrieve all information.
		SearchResponse response =  client.prepareSearch("etm_event_all")
			.setQuery(idsQueryBuilder)
			.setFetchSource(new String[] {this.eventTags.getEndpointsTag() + ".*"}, null)
			.setFrom(0)
			.setSize(1)
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.get();
		if (response.getHits().hits().length == 0) {
			return null;
		}
		SearchHit searchHit = response.getHits().getAt(0);
		Map<String, Object> source = searchHit.getSource();
		List<Map<String, Object>> endpoints = (List<Map<String, Object>>) source.get(this.eventTags.getEndpointsTag());
		// Search for the earliest transaction id.
		long lowestTransactionHandling = Long.MAX_VALUE;
		String earliestTransactionId = null;
		if (endpoints != null) {
			for (Map<String, Object> endpoint : endpoints) {
				Map<String, Object> writingEndpointHandler = (Map<String, Object>) endpoint.get(this.eventTags.getWritingEndpointHandlerTag());
				if (writingEndpointHandler != null && writingEndpointHandler.containsKey(this.eventTags.getEndpointHandlerTransactionIdTag())) {
					String transactionId = (String) writingEndpointHandler.get(this.eventTags.getEndpointHandlerTransactionIdTag());
					long handlingTime = (long) writingEndpointHandler.get(this.eventTags.getEndpointHandlerHandlingTimeTag());
					if (handlingTime != 0 && handlingTime < lowestTransactionHandling) {
						lowestTransactionHandling = handlingTime;
						earliestTransactionId = transactionId;
					}
				}
				List<Map<String, Object>> readingEndpointHandlers = (List<Map<String, Object>>) endpoint.get(this.eventTags.getReadingEndpointHandlersTag());
				if (readingEndpointHandlers != null) {
					for (Map<String, Object> readingEndpointHandler : readingEndpointHandlers) {
						if (readingEndpointHandler.containsKey(this.eventTags.getEndpointHandlerTransactionIdTag())) {
							String transactionId = (String) readingEndpointHandler.get(this.eventTags.getEndpointHandlerTransactionIdTag());
							long handlingTime = (long) readingEndpointHandler.get(this.eventTags.getEndpointHandlerHandlingTimeTag());
							if (handlingTime != 0 && handlingTime < lowestTransactionHandling) {
								lowestTransactionHandling = handlingTime;
								earliestTransactionId = transactionId;
							}					
						}
					}
				}
			}
		}
		if (earliestTransactionId == null)  {
			return null;
		}
		EventChain eventChain = new EventChain();
		addTransactionToEventChain(eventChain, earliestTransactionId);
		eventChain.done();
		StringBuilder result = new StringBuilder();
		result.append("{");
		result.append("\"nodes\" : [");
		boolean first = true;
		// Add all applications as item.
		for (String application : eventChain.getApplications()) {
			if (!first) {
				result.append(",");
			}
			result.append("{\"id\": " + escapeToJson(application, true) + ", \"label\": " + escapeToJson(application, true) + ", \"type\": \"application\", \"missing\": " + eventChain.isApplicationMissing(application) + "}");
			first = false;
		}
		for (EventChainEvent event : eventChain.events.values()) {
			// Add all endpoints as item.
			if (event.getEndpointName() != null) {
				if (!first) {
					result.append(",");
				}
				result.append("{\"id\": " + escapeToJson(event.getEventId(), true) + ", \"label\": " + escapeToJson(event.getEndpointName(), true) + ", \"type\": \"endpoint\", \"missing\": " + event.isMissing() + "}");
				first = false;				
			}
			// Add the reader as item.
			if (event.getWriter() != null) {
				if (!first) {
					result.append(",");
				}
				result.append("{\"id\": " + escapeToJson(event.getWriter().getKey(), true) + ", \"label\": " + escapeToJson(event.getWriter().getName(), true) + ", \"type\": \"event\", \"missing\": " + event.getWriter().isMissing());
				if (event.getWriter().getApplicationName() != null) {
					result.append(", \"parent\": " + escapeToJson(event.getWriter().getApplicationName(), true)); 
				}
				result.append("}");
				first = false;			
			}
			// Add all writers as item.
			for (EventChainItem item : event.getReaders()) {
				if (!first) {
					result.append(",");
				}
				result.append("{\"id\": " + escapeToJson(item.getKey(), true) + ", \"label\": " + escapeToJson(item.getName(), true) + ", \"type\": \"event\", \"missing\": " + item.isMissing());
				if (item.getApplicationName() != null) {
					result.append(", \"parent\": " + escapeToJson(item.getApplicationName(), true)); 
				}
				result.append("}");
				first = false;							
			}
		}
		result.append("], \"edges\": [");
		first = true;
		for (EventChainEvent event : eventChain.events.values()) {
			if (event.getEndpointName() != null) {
				if (event.getWriter() != null) {
					if (!first) {
						result.append(",");
					}
					// Add the connection from the writer to the endpoint.
					result.append("{\"source\": " + escapeToJson(event.getWriter().getKey(), true) 
							+ ", \"target\": " + escapeToJson(event.getEventId(), true));
					if (!event.getWriter().isMissing()) {
						result.append(", \"transition_time_percentage\": 0.0");
					}
					result.append("}");
					first = false;			
				}
				for (EventChainItem item : event.getReaders()) {
					// Add a connection between the endpoint and all readers.
					if (!first) {
						result.append(",");
					}
					result.append("{ \"source\": " +  escapeToJson(event.getEventId(), true)
						+ ", \"target\": " + escapeToJson(item.getKey(), true)); 
					Float edgePercentage = eventChain.calculateEdgePercentageFromEndpointToItem(event, item);
					if (edgePercentage != null) {
						result.append(", \"transition_time_percentage\": " + edgePercentage);
					}
					result.append("}");
					first = false;							
				}
			} else {
				// No endpoint name, so a direct connection from a writer to the readers.
				if (event.getWriter() != null) {
					for (EventChainItem item : event.getReaders()) {
						// Add a connection between the writer and all readers.
						if (!first) {
							result.append(",");
						}
						result.append("{ \"source\": " +  escapeToJson(event.getWriter().getKey(), true)
							+ ", \"target\": " + escapeToJson(item.getKey(), true)); 
						Float edgePercentage = eventChain.calculateEdgePercentageFromEndpointToItem(event, item);
						if (edgePercentage != null) {
							result.append(", \"transition_time_percentage\": " + edgePercentage);
						}						
						result.append("}");
						first = false;							
					}					
				}
			}
		}
		for (EventChainTransaction transaction : eventChain.transactions.values()) {
			// Add connections between the events within a transaction
			int writerIx = 0;
			for (int i=0; i < transaction.getReaders().size(); i++) {
				long endTime = Long.MAX_VALUE;
				if ((i + 1) < transaction.getReaders().size()) {
					endTime = transaction.getReaders().get(i + 1).getHandlingTime();
				}
				EventChainItem reader = transaction.getReaders().get(i);
				for (; writerIx < transaction.getWriters().size() && transaction.getWriters().get(writerIx).getHandlingTime() < endTime; writerIx++) {
					if (!first) {
						result.append(",");
					}
					result.append("{ \"source\": " + escapeToJson(reader.getKey(), true) + ", \"target\": " + escapeToJson(transaction.getWriters().get(writerIx).getKey(), true));
					Float edgePercentage = eventChain.calculateEdgePercentageFromItemToItem(reader, transaction.getWriters().get(writerIx));
					if (edgePercentage != null) {
						result.append(", \"transition_time_percentage\": " + edgePercentage);
					}						
					result.append("}");					
					first = false;												
				}
			}
			
		}
		result.append("]}");
		return result.toString();
	}

	private void addTransactionToEventChain(EventChain eventChain, String transactionId) {
		if (eventChain.containsTransaction(transactionId)) {
			return;
		}
		BoolQueryBuilder findEventsQuery = new BoolQueryBuilder()
				.minimumNumberShouldMatch(1)
				.should(new TermQueryBuilder(this.eventTags.getEndpointsTag() + 
						"." + this.eventTags.getReadingEndpointHandlersTag() + 
						"." + this.eventTags.getEndpointHandlerTransactionIdTag(), transactionId))
				.should(new TermQueryBuilder(this.eventTags.getEndpointsTag() + 
						"." + this.eventTags.getWritingEndpointHandlerTag() + 
						"." + this.eventTags.getEndpointHandlerTransactionIdTag(), transactionId));
		final int scrollSize = 25;
		// No principal filtered query. We would like to show the entire event chain, but the user should not be able to retrieve all information.
		SearchResponse response =  client.prepareSearch("etm_event_all")
				.setTypes("http", "messaging")
				.setQuery(findEventsQuery)
				.addSort(SortBuilders.fieldSort("_doc"))
				.setFetchSource(new String[] {
						this.eventTags.getEndpointsTag() + ".*",
						this.eventTags.getExpiryTag(),
						this.eventTags.getNameTag(), 
						this.eventTags.getCorrelationIdTag(),
						this.eventTags.getMessagingEventTypeTag(),
						this.eventTags.getHttpEventTypeTag()}, null)
				.setFrom(0)
				.setSize(scrollSize)
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
				.setScroll(new Scroll(TimeValue.timeValueSeconds(60)))
				.get();
		if (response.getHits().hits().length == 0) {
			return;
		}
		Set<String> scrollIds = new HashSet<>();
		String scrollId = response.getScrollId();
		scrollIds.add(scrollId);
		boolean nextBatchRequired = false;
		do {
			nextBatchRequired = scrollSize == response.getHits().hits().length;
			for (SearchHit searchHit : response.getHits().hits()) {
				Map<String, Object> source = searchHit.getSource();
				String eventName = getString(this.eventTags.getNameTag(), source, "?");
				Long expiry = getLong(this.eventTags.getExpiryTag(), source);
				String subType = null;
				if ("http".equals(searchHit.getType())) {
					subType = getString(this.eventTags.getHttpEventTypeTag(), source);
				} else if ("messaging".equals(searchHit.getType())) {
					subType = getString(this.eventTags.getMessagingEventTypeTag(), source);
				}
				String correlationId = getString(this.eventTags.getCorrelationIdTag(), source);
				List<Map<String, Object>> endpoints =  getArray(this.eventTags.getEndpointsTag(), source);
				if (endpoints != null) {
					for (Map<String, Object> endpoint : endpoints) {
						String endpointName = getString(this.eventTags.getEndpointNameTag(), endpoint);
						Map<String, Object> writingEndpointHandler = getObject(this.eventTags.getWritingEndpointHandlerTag(), endpoint);
						processEndpointHandlerForEventChain(eventChain, 
								writingEndpointHandler, 
								true, 
								searchHit.getId(), 
								eventName, 
								searchHit.getType(), 
								correlationId, 
								subType,
								endpointName, 
								transactionId, 
								expiry);
						List<Map<String, Object>> readingEndpointHandlers =  getArray(this.eventTags.getReadingEndpointHandlersTag(), endpoint);
						if (readingEndpointHandlers != null) {
							for (Map<String, Object> readingEndpointHandler : readingEndpointHandlers) {
								processEndpointHandlerForEventChain(eventChain, 
										readingEndpointHandler, 
										false, 
										searchHit.getId(), 
										eventName, 
										searchHit.getType(), 
										correlationId,
										subType,
										endpointName, 
										transactionId, 
										expiry);
							}
						}
					}
				}
				// Check for request/response correlation and add those transactions as well.
				addRequestResponseConnectionToEventChain(eventChain, searchHit.getId(), correlationId, searchHit.getType(), subType);
			}
			if (nextBatchRequired) {
				// Full batch fetched, request the next batch.
				response = client.prepareSearchScroll(scrollId)
						.setScroll(TimeValue.timeValueSeconds(30))
						.get(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
				scrollId = response.getScrollId();
				scrollIds.add(scrollId);
			}
		} while (nextBatchRequired);
	}
	
	@SuppressWarnings("unchecked")
	private void processEndpointHandlerForEventChain(EventChain eventChain, 
			Map<String, Object> endpointHandler,
			boolean writer, 
			String eventId, 
			String eventName, 
			String eventType, 
			String correlationId, 
			String subType, 
			String endpointName,
			String transactionId, 
			Long eventExpiry) {
		if (endpointHandler != null && endpointHandler.containsKey(this.eventTags.getEndpointHandlerTransactionIdTag())) {
			String handlerTransactionId = getString(this.eventTags.getEndpointHandlerTransactionIdTag(), endpointHandler);
			long handlingTime = getLong(this.eventTags.getEndpointHandlerHandlingTimeTag(), endpointHandler);
			String applicationName = null;
			if (endpointHandler.containsKey(this.eventTags.getEndpointHandlerApplicationTag())) {
				Map<String, Object> application = (Map<String, Object>) endpointHandler.get(this.eventTags.getEndpointHandlerApplicationTag());
				applicationName = getString(this.eventTags.getApplicationNameTag(), application);
			}
			Long responseTime = getLong(this.eventTags.getEndpointHandlerResponseTimeTag(), endpointHandler);
			if (transactionId.equals(handlerTransactionId)) {
				if (writer) {
					eventChain.addWriter(eventId, transactionId, eventName, eventType, correlationId, subType, endpointName, applicationName, handlingTime, responseTime, eventExpiry);
				} else {
					eventChain.addReader(eventId, transactionId, eventName, eventType, correlationId, subType, endpointName, applicationName, handlingTime, responseTime, eventExpiry);
				}
			} else if (!eventChain.containsTransaction(handlerTransactionId)) {
				addTransactionToEventChain(eventChain, handlerTransactionId);
			}
		}
	}
	
	private void addRequestResponseConnectionToEventChain(EventChain eventChain, String id, String correlationId, String type, String subType) {
		QueryBuilder queryBuilder = null;
		// TODO, it's possible to search more accurate -> in case of a request search for a response.
		if ("messaging".equals(type)) {
			MessagingEventType messagingEventType = MessagingEventType.safeValueOf(subType);
			if (MessagingEventType.REQUEST.equals(messagingEventType)) {
				queryBuilder = new BoolQueryBuilder().must(new TermQueryBuilder(this.eventTags.getCorrelationIdTag(), id));
			} else if (MessagingEventType.RESPONSE.equals(messagingEventType) && correlationId != null) {
				queryBuilder = new IdsQueryBuilder("messaging").addIds(correlationId);
			}
		} else if ("http".equals("type")) {
			HttpEventType httpEventType = HttpEventType.safeValueOf(subType);
			if (HttpEventType.RESPONSE.equals(httpEventType) && correlationId != null) {
				queryBuilder = new IdsQueryBuilder("http").addIds(correlationId);
			} else {
				queryBuilder = new BoolQueryBuilder().must(new TermQueryBuilder(this.eventTags.getCorrelationIdTag(), id));
			}
		}
		if (queryBuilder == null) {
			return;
		}
		SearchResponse response =  client.prepareSearch("etm_event_all")
				.setTypes(type)
				.setQuery(queryBuilder)
				.setFetchSource(new String[] {this.eventTags.getEndpointsTag() + ".*"}, null)
				.setFrom(0)
				.setSize(10)
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
				.get();
		if (response.getHits().hits().length == 0) {
			return;
		}
		for (SearchHit searchHit : response.getHits().hits()) {
			Map<String, Object> source = searchHit.getSource();
			List<Map<String, Object>> endpoints =  getArray(this.eventTags.getEndpointsTag(), source);
			if (endpoints == null) {
				continue;
			}
			for (Map<String, Object> endpoint : endpoints) {
				Map<String, Object> writingEndpointHandler = getObject(this.eventTags.getWritingEndpointHandlerTag(), endpoint);
				String transactionId = getString(this.eventTags.getEndpointHandlerTransactionIdTag(), writingEndpointHandler);
				if (transactionId != null) {
					addTransactionToEventChain(eventChain, transactionId);
				}
				List<Map<String, Object>> readingEndpointHandlers =  getArray(this.eventTags.getReadingEndpointHandlersTag(), endpoint);
				if (readingEndpointHandlers != null) {
					for (Map<String, Object> readingEndpointHandler : readingEndpointHandlers) {
						transactionId = getString(this.eventTags.getEndpointHandlerTransactionIdTag(), readingEndpointHandler);
						if (transactionId != null) {
							addTransactionToEventChain(eventChain, transactionId);
						}
					}
				}
			}
		}
		
	}

	
	@SuppressWarnings("unchecked")
	private boolean isWithinTransaction(Map<String, Object> endpointHandler, String applicationName, String id) {
		if (endpointHandler == null) {
			return false;
		}
		String transactionId = (String) endpointHandler.get(this.eventTags.getEndpointHandlerTransactionIdTag());
		if (!id.equals(transactionId)) {
			return false;
		}
		Map<String, Object> application = (Map<String, Object>) endpointHandler.get(this.eventTags.getEndpointHandlerApplicationTag());
		if (application == null) {
			return false;
		}
		String appName = (String) application.get(this.eventTags.getApplicationNameTag());
		return applicationName.equals(appName);
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
	
	private void clearScrolls(Collection<String> scrollIds) {
		ClearScrollRequestBuilder clearScroll = client.prepareClearScroll();
		for (String scrollId : scrollIds) {
			clearScroll.addScrollId(scrollId);
		}
		clearScroll.execute();
	}

	@SuppressWarnings("unchecked")
	private void addProperties(List<String> names, String prefix, Map<String, Object> valueMap) {
		valueMap = getObject("properties", valueMap);
		if (valueMap == null) {
			return;
		}
		// Remove temp fields that are used for correlating events.
		valueMap.remove("temp_for_correlations");
		// Remove event hashes field. It is used for internal handling of events.
		valueMap.remove("event_hashes");
		for (Entry<String, Object> entry : valueMap.entrySet()) {
			Map<String, Object> entryValues = (Map<String, Object>) entry.getValue();
			String name = determinePropertyName(prefix, entry.getKey());
			if (entryValues.containsKey("properties")) {
				addProperties(names, name, entryValues);
			} else {
				if (!names.contains(name)) {
					names.add(name);
				}
			}
		}
	}
	
	private String determinePropertyName(String prefix, String name) {
		if (prefix.length() == 0) {
			return name;
		}
		return prefix + "." + name;
	}

}
