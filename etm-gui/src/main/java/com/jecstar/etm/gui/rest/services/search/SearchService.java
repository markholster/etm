package com.jecstar.etm.gui.rest.services.search;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTimeZone;

import com.jecstar.etm.domain.HttpTelemetryEvent.HttpEventType;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.writers.TelemetryEventTags;
import com.jecstar.etm.domain.writers.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.gui.rest.services.AbstractIndexMetadataService;
import com.jecstar.etm.gui.rest.services.Keyword;
import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.EtmGroup;
import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.audit.GetEventAuditLog;
import com.jecstar.etm.server.core.domain.audit.QueryAuditLog;
import com.jecstar.etm.server.core.domain.audit.builders.GetEventAuditLogBuilder;
import com.jecstar.etm.server.core.domain.audit.builders.QueryAuditLogBuilder;
import com.jecstar.etm.server.core.domain.converter.AuditLogConverter;
import com.jecstar.etm.server.core.domain.converter.json.GetEventAuditLogConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.QueryAuditLogConverterJsonImpl;
import com.jecstar.etm.server.core.util.DateUtils;

@Path("/search")
public class SearchService extends AbstractIndexMetadataService {

	private static final DateTimeFormatter dateTimeFormatterIndexPerDay = DateUtils.getIndexPerDayFormatter();
	private static Client client;
	private static EtmConfiguration etmConfiguration;
	
	private final TelemetryEventTags eventTags = new TelemetryEventTagsJsonImpl();
	private final QueryExporter queryExporter = new QueryExporter();
	private final AuditLogConverter<String, QueryAuditLog> queryAuditLogConverter = new QueryAuditLogConverterJsonImpl();
	private final AuditLogConverter<String, GetEventAuditLog> getEventAuditLogConverter = new GetEventAuditLogConverterJsonImpl();
	
	
	public static void initialize(Client client, EtmConfiguration etmConfiguration) {
		SearchService.client = client;
		SearchService.etmConfiguration = etmConfiguration;
	}
	
	@GET
	@Path("/templates")
	@Produces(MediaType.APPLICATION_JSON)
	public String getSearchTemplates() {
		GetResponse getResponse = SearchService.client.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, getEtmPrincipal().getId())
				.setFetchSource("search_templates", null)
				.get();
		if (getResponse.isSourceEmpty() || getResponse.getSourceAsMap().isEmpty()) {
			return "{\"max_search_templates\": " + etmConfiguration.getMaxSearchTemplateCount() + "}";
		}
		// Hack the max search history into the result. Dunno how to do this better.
		StringBuilder result = new StringBuilder(getResponse.getSourceAsString().substring(0, getResponse.getSourceAsString().lastIndexOf("}")));
		addIntegerElementToJsonBuffer("max_search_templates", etmConfiguration.getMaxSearchTemplateCount(), result, false);
		result.append("}");
		return result.toString();
	}
	
	@GET
	@Path("/history")
	@Produces(MediaType.APPLICATION_JSON)
	public String getRecentQueries() {
		EtmPrincipal etmPrincipal = getEtmPrincipal();
		GetResponse getResponse = SearchService.client.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, etmPrincipal.getId())
				.setFetchSource("search_history", null)
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
		scriptParams.put("max_templates", etmConfiguration.getMaxSearchTemplateCount());
		SearchService.client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, getEtmPrincipal().getId())
				.setScript(new Script(ScriptType.STORED, "painless", "etm_update-search-template", scriptParams))
				.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
				.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
				.get();
		return "{ \"status\": \"success\" }";
	}

	@DELETE
	@Path("/templates/{templateName}")
	@Produces(MediaType.APPLICATION_JSON)
	public String removeSearchTemplate(@PathParam("templateName") String templateName) {
		Map<String, Object> scriptParams = new HashMap<String, Object>();
		scriptParams.put("name", templateName);
		SearchService.client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, getEtmPrincipal().getId())
			.setScript(new Script(ScriptType.STORED, "painless", "etm_remove-search-template", scriptParams))
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
			.get();
		return "{ \"status\": \"success\" }";
	}
	
	@GET
	@Path("/keywords/{indexName}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getKeywords(@PathParam("indexName") String indexName) {
		StringBuilder result = new StringBuilder();
		Map<String, List<Keyword>> names = getIndexFields(SearchService.client, indexName);
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
				result.append("\"keywords\": [" + entry.getValue().stream().map(n -> 
					{StringBuilder kw = new StringBuilder();
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
		
		QueryAuditLogBuilder auditLogBuilder = new QueryAuditLogBuilder().setHandlingTime(ZonedDateTime.now()).setPrincipalId(etmPrincipal.getId());
		
		SearchRequestParameters parameters = new SearchRequestParameters(toMap(json));
		SearchRequestBuilder requestBuilder = createRequestFromInput(parameters, etmPrincipal);
		NumberFormat numberFormat = NumberFormat.getInstance(etmPrincipal.getLocale());
		SearchResponse response = requestBuilder.get();
		StringBuilder result = new StringBuilder();
		result.append("{");
		result.append("\"status\": \"success\"");
		result.append(",\"history_size\": " + etmPrincipal.getHistorySize());
		result.append(",\"hits\": " + response.getHits().getTotalHits());
		result.append(",\"hits_as_string\": \"" + numberFormat.format(response.getHits().getTotalHits()) + "\"");
		result.append(",\"time_zone\": \"" + etmPrincipal.getTimeZone().getID() + "\"");
		result.append(",\"start_ix\": " + parameters.getStartIndex());
		result.append(",\"end_ix\": " + (parameters.getStartIndex() + response.getHits().hits().length - 1));
		result.append(",\"has_more_results\": " + (parameters.getStartIndex() + response.getHits().hits().length < response.getHits().getTotalHits() - 1));
		result.append(",\"time_zone\": \"" + etmPrincipal.getTimeZone().getID() + "\"");
		result.append(",\"max_downloads\": " + etmConfiguration.getMaxSearchResultDownloadRows());
		result.append(",\"results\": [");
		addSearchHits(result, response.getHits());
		result.append("]");
		long queryTime = System.currentTimeMillis() - startTime;
		result.append(",\"query_time\": " + queryTime);
		result.append(",\"query_time_as_string\": \"" + numberFormat.format(queryTime) + "\"");
		result.append("}");
		
		if (parameters.getStartIndex() == 0) {
			writeQueryHistory(startTime,  
					parameters, 
					etmPrincipal, 
					Math.min(etmPrincipal.getHistorySize(), etmConfiguration.getMaxSearchHistoryCount()));
			// Log the query request to the audit logs.
			String executedQuery = null;
			try {
				XContentBuilder contentBuilder = XContentFactory.jsonBuilder();
				requestBuilder.request().source().query().toXContent(contentBuilder, ToXContent.EMPTY_PARAMS);
				executedQuery = contentBuilder.string();
			} catch (IOException e) {
				// TODO error logging.
			}
			auditLogBuilder.setUserQuery(parameters.getQueryString()).setExectuedQuery(executedQuery).setNumberOfResults(response.getHits().getTotalHits());
			client.prepareIndex(ElasticSearchLayout.ETM_AUDIT_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(ZonedDateTime.now()), ElasticSearchLayout.ETM_AUDIT_INDEX_TYPE_SEARCH)
				.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
				.setSource(this.queryAuditLogConverter.write(auditLogBuilder.build()), XContentType.JSON)
				.execute();
		}
		return result.toString();
	}
	
	
	
	private SearchRequestBuilder createRequestFromInput(SearchRequestParameters parameters, EtmPrincipal etmPrincipal) {
		if (parameters.getFields().isEmpty()) {
			parameters.getFields().add(this.eventTags.getEndpointsTag() + "." + this.eventTags.getWritingEndpointHandlerTag() + "." + this.eventTags.getEndpointHandlerHandlingTimeTag());
			parameters.getFields().add(this.eventTags.getNameTag());
		}
		QueryStringQueryBuilder queryStringBuilder = new QueryStringQueryBuilder(parameters.getQueryString())
			.allowLeadingWildcard(true)
			.analyzeWildcard(true)
			.defaultField("_all")
			.timeZone(DateTimeZone.forTimeZone(etmPrincipal.getTimeZone()));
		BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
		boolQueryBuilder.must(queryStringBuilder);
		boolQueryBuilder.filter(new RangeQueryBuilder("timestamp").lte(parameters.getNotAfterTimestamp()));
		SearchRequestBuilder requestBuilder = client.prepareSearch(ElasticSearchLayout.ETM_EVENT_INDEX_ALIAS_ALL)
			.setQuery(addEtmPrincipalFilterQuery(boolQueryBuilder))
			.setFetchSource(parameters.getFields().toArray(new String[parameters.getFields().size()]), null)
			.setFrom(parameters.getStartIndex())
			.setSize(parameters.getMaxResults() > 500 ? 500 : parameters.getMaxResults())
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		if (parameters.getSortField() != null && parameters.getSortField().trim().length() > 0) {
			requestBuilder.addSort(parameters.getSortField(), "desc".equals(parameters.getSortOrder()) ? SortOrder.DESC : SortOrder.ASC);
		}
		if (parameters.getTypes() != null && !parameters.getTypes().isEmpty()) {
			requestBuilder.setTypes(parameters.getTypes().toArray(new String[parameters.getTypes().size()]));
		}
		return requestBuilder;
	}
	
	private void writeQueryHistory(long timestamp, SearchRequestParameters parameters, EtmPrincipal etmPrincipal, int history_size) {
		Map<String, Object> scriptParams = new HashMap<String, Object>();
		Map<String, Object> query = new HashMap<String, Object>();
		query.put("timestamp", timestamp);
		query.put("query", parameters.getQueryString());
		query.put("types", parameters.getTypes());
		query.put("fields", parameters.getFieldsLayout());
		query.put("results_per_page", parameters.getMaxResults());
		query.put("sort_field", parameters.getSortField());
		query.put("sort_order", parameters.getSortOrder());
		
		scriptParams.put("query", query);
		scriptParams.put("history_size", history_size);
		SearchService.client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, getEtmPrincipal().getId())
				.setScript(new Script(ScriptType.STORED, "painless", "etm_update-search-history", scriptParams))
				.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
				.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
				.execute();
	}
	
	@GET
	@Path("/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getDownload(@QueryParam("q") String json) {
		EtmPrincipal etmPrincipal = getEtmPrincipal(); 
		Map<String, Object> valueMap = toMap(json);
		SearchRequestParameters parameters = new SearchRequestParameters(valueMap);
		if (getBoolean("includePayload", valueMap) && !parameters.getFields().contains(this.eventTags.getPayloadTag())) {
			parameters.getFields().add(this.eventTags.getPayloadTag());
			Map<String, Object> payloadFieldLayout = new HashMap<>();
			payloadFieldLayout.put("name", "Payload");
			payloadFieldLayout.put("field", "payload");
			payloadFieldLayout.put("format", "plain");
			payloadFieldLayout.put("array", "first");
			parameters.getFieldsLayout().add(payloadFieldLayout);
			
		}
		SearchRequestBuilder requestBuilder = createRequestFromInput(parameters, etmPrincipal);
		
		ScrollableSearch scrollableSearch = new ScrollableSearch(client, requestBuilder, parameters.getStartIndex());
		String fileType = getString("fileType", valueMap);
		File result = this.queryExporter.exportToFile(scrollableSearch, fileType, Math.min(parameters.getMaxResults(), etmConfiguration.getMaxSearchResultDownloadRows()), parameters, etmPrincipal);
	    scrollableSearch.clearScrollIds();
		ResponseBuilder response = Response.ok(result);
	    response.header("Content-Disposition", "attachment; filename=etm-results." + fileType);
	    response.encoding(System.getProperty("file.encoding"));
	    response.header("Content-Type", this.queryExporter.getContentType(fileType));
	    return response.build();

	}

	@GET
	@Path("/event/{type}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getEvent(@PathParam("type") String eventType, @PathParam("id") String eventId) {
		GetEventAuditLogBuilder auditLogBuilder = new GetEventAuditLogBuilder()
			.setHandlingTime(ZonedDateTime.now())
			.setPrincipalId(getEtmPrincipal().getId())
			.setEventId(eventId)
			.setEventType(eventType)
			.setFound(false);
		StringBuilder result = new StringBuilder();
		result.append("{");
		addStringElementToJsonBuffer("time_zone", getEtmPrincipal().getTimeZone().getID() , result, true);
		SearchHit searchHit = getEvent(eventType, eventId, true, null, null);
		if (searchHit != null) {
			auditLogBuilder.setFound(true);
			result.append(", \"event\": {");
			addStringElementToJsonBuffer("index", searchHit.getIndex() , result, true);
			addStringElementToJsonBuffer("type", searchHit.getType() , result, false);
			addStringElementToJsonBuffer("id", searchHit.getId() , result, false);
			result.append(", \"source\": " + searchHit.getSourceAsString());
			result.append("}");

			// Try to find an event this event is correlating to.
			Map<String, Object> valueMap = searchHit.getSource();
			// Add the name to the audit log.
			auditLogBuilder.setEventName(getString(this.eventTags.getNameTag(), valueMap));
			String correlatedToId = getString(this.eventTags.getCorrelationIdTag(), valueMap);
			boolean correlationAdded = false;
			if (correlatedToId != null && !correlatedToId.equals(eventId)) {
				SearchHit correlatedEvent = conditionallyGetEvent(eventType, correlatedToId);
				if (correlatedEvent != null) {
					if (!correlationAdded) {
						result.append(", \"correlated_events\": [");
					}
					result.append("{");
					addStringElementToJsonBuffer("index", correlatedEvent.getIndex() , result, true);
					addStringElementToJsonBuffer("type", correlatedEvent.getType() , result, false);
					addStringElementToJsonBuffer("id", correlatedEvent.getId() , result, false);
					result.append(", \"source\": " + correlatedEvent.getSourceAsString());
					result.append("}");
					correlationAdded = true;
				}
			}
			// Try to find event that correlate to this event.
			List<String> correlations = getArray(this.eventTags.getCorrelationsTag(), valueMap);
			if (correlations != null && !correlations.isEmpty()) {
				int added = 0;
				for (int i = 0; i < correlations.size() &&  added <= 10; i++) {
					String correlationId = correlations.get(i);
					if (eventId.equals(correlationId)) {
						// An event correlates to itself.
						continue;
					}
					SearchHit correlatedEvent = conditionallyGetEvent(eventType, correlationId);					
					if (correlatedEvent != null) {
						added++;
						if (!correlationAdded) {
							result.append(", \"correlated_events\": [");
						} else {
							result.append(",");
						}
						result.append("{");
						addStringElementToJsonBuffer("index", correlatedEvent.getIndex() , result, true);
						addStringElementToJsonBuffer("type", correlatedEvent.getType() , result, false);
						addStringElementToJsonBuffer("id", correlatedEvent.getId() , result, false);
						result.append(", \"source\": " + correlatedEvent.getSourceAsString());
						result.append("}");
						correlationAdded = true;
					}
				}
			}
			if (correlationAdded) {
				result.append("]");
			}
			
		}
		result.append("}");
		// Log the retrieval request to the audit logs.
		client.prepareIndex(ElasticSearchLayout.ETM_AUDIT_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(ZonedDateTime.now()), ElasticSearchLayout.ETM_AUDIT_INDEX_TYPE_GET_EVENT)
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setSource(this.getEventAuditLogConverter.write(auditLogBuilder.build()), XContentType.JSON)
			.execute();
		return result.toString();
	}
	
	@GET
	@Path("/event/{type}/{id}/endpoints")
	@Produces(MediaType.APPLICATION_JSON)
	public String getEventChainEndpoint(@PathParam("type") String eventType, @PathParam("id") String eventId) {
		StringBuilder result = new StringBuilder();
		result.append("{");
		addStringElementToJsonBuffer("time_zone", getEtmPrincipal().getTimeZone().getID() , result, true);
		SearchHit searchHit = getEvent(eventType, eventId, false, new String[] {this.eventTags.getEndpointsTag() + ".*"}, null);
		if (searchHit != null) {
			result.append(", \"event\": {");
			addStringElementToJsonBuffer("index", searchHit.getIndex() , result, true);
			addStringElementToJsonBuffer("type", searchHit.getType() , result, false);
			addStringElementToJsonBuffer("id", searchHit.getId() , result, false);
			result.append(", \"source\": " + searchHit.getSourceAsString());
			result.append("}");
		}
		result.append("}");
		return result.toString();
	}
	
	private SearchHit getEvent(String eventType, String eventId, boolean fetchAll, String[] includes, String[] excludes) {
		IdsQueryBuilder idsQueryBuilder = new IdsQueryBuilder()
				.types(eventType)
				.addIds(eventId);
		SearchRequestBuilder builder = client.prepareSearch(ElasticSearchLayout.ETM_EVENT_INDEX_ALIAS_ALL)
			.setQuery(addEtmPrincipalFilterQuery(idsQueryBuilder))
			.setFrom(0)
			.setSize(1)
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		if (fetchAll) {
			builder.setFetchSource(true);
		} else {
			builder.setFetchSource(includes, excludes);
		}
		SearchResponse response = builder.get();
		if (response.getHits().hits().length == 0) {
			return null;
		}
		return response.getHits().getAt(0);
	}
	
	private SearchHit conditionallyGetEvent(String eventType, String eventId) {
		IdsQueryBuilder idsQueryBuilder = new IdsQueryBuilder()
				.types(eventType)
				.addIds(eventId);
		SearchRequestBuilder builder = client.prepareSearch(ElasticSearchLayout.ETM_EVENT_INDEX_ALIAS_ALL)
			.setQuery(alwaysShowCorrelatedEvents(getEtmPrincipal()) ? idsQueryBuilder : addEtmPrincipalFilterQuery(idsQueryBuilder))
			.setFrom(0)
			.setSize(1)
			.setFetchSource(true)
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		SearchResponse response = builder.get();
		if (response.getHits().hits().length == 0) {
			return null;
		}
		return response.getHits().getAt(0);		
	}
	
	private boolean alwaysShowCorrelatedEvents(EtmPrincipal etmPrincipal) {
		if (!hasFilterQueries()) {
			// no filter queries, user is allowed to view everything.
			return true;
		}
		boolean showCorrelatedEvents = etmPrincipal.isAlwaysShowCorrelatedEvents();
		Iterator<EtmGroup> it = etmPrincipal.getGroups().iterator();
		while ((!showCorrelatedEvents) && it.hasNext()) {
			showCorrelatedEvents = it.next().isAlwaysShowCorrelatedEvents();
		}
		return showCorrelatedEvents;
	}
	
	@GET
	@Path("/transaction/{application}/{id}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getTransaction(@PathParam("application") String applicationName, @PathParam("id") String transactionId) {
		BoolQueryBuilder findEventsQuery = new BoolQueryBuilder()
			.minimumShouldMatch(1)
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
		
		
		SearchRequestBuilder searchRequest = client.prepareSearch(ElasticSearchLayout.ETM_EVENT_INDEX_ALIAS_ALL)
			.setQuery(addEtmPrincipalFilterQuery(findEventsQuery))
			.addSort(SortBuilders.fieldSort("_doc"))
			.setFetchSource(new String[] {
					this.eventTags.getEndpointsTag() + ".*", 
					this.eventTags.getNameTag(), 
					this.eventTags.getPayloadTag(),
					this.eventTags.getMessagingEventTypeTag(),
					this.eventTags.getHttpEventTypeTag(),
					this.eventTags.getSqlEventTypeTag()}, null)
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequest);
		if (!scrollableSearch.hasNext()) {
			scrollableSearch.clearScrollIds();
			return null;
		}
		List<TransactionEvent> events = new ArrayList<>();
		StringBuilder result = new StringBuilder();
		for (SearchHit searchHit : scrollableSearch) {
			TransactionEvent event = new TransactionEvent();
			event.index = searchHit.getIndex();
			event.type = searchHit.getType();
			event.id = searchHit.getId();
			Map<String, Object> source = searchHit.getSource();
			event.name = getString(this.eventTags.getNameTag(), source);
			event.payload = getString(this.eventTags.getPayloadTag(), source);
			List<Map<String, Object>> endpoints =  getArray(this.eventTags.getEndpointsTag(), source);
			if (endpoints != null) {
				for (Map<String, Object> endpoint : endpoints) {
					Map<String, Object> writingEndpointHandler = (Map<String, Object>) getObject(this.eventTags.getWritingEndpointHandlerTag(), endpoint);
					if (isWithinTransaction(writingEndpointHandler, applicationName, transactionId)) {
						event.handlingTime = getLong(this.eventTags.getEndpointHandlerHandlingTimeTag(), writingEndpointHandler);
						event.direction = "outgoing";
						event.endpoint = getString(this.eventTags.getEndpointNameTag(), endpoint);
					} else {
						List<Map<String, Object>> readingEndpointHandlers = getArray(this.eventTags.getReadingEndpointHandlersTag(), endpoint);
						if (readingEndpointHandlers != null) {
							for (Map<String, Object> readingEndpointHandler : readingEndpointHandlers) {
								if (isWithinTransaction(readingEndpointHandler, applicationName, transactionId)) {
									event.handlingTime = getLong(this.eventTags.getEndpointHandlerHandlingTimeTag(), readingEndpointHandler);
									event.direction = "incoming";
									event.endpoint = getString(this.eventTags.getEndpointNameTag(), endpoint);
								}
							}
						}
					}
					if ("http".equals(searchHit.getType())) {
						event.subType = getString(this.eventTags.getHttpEventTypeTag(), source);
					} else if ("messaging".equals(searchHit.getType())) {
						event.subType = getString(this.eventTags.getMessagingEventTypeTag(), source);
					} else if ("sql".equals(searchHit.getType())) {
						event.subType = getString(this.eventTags.getSqlEventTypeTag(), source);
					}
				}
			}
			events.add(event);
		}
		scrollableSearch.clearScrollIds();
		
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
			addStringElementToJsonBuffer("endpoint", event.endpoint , result, false);
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
		IdsQueryBuilder idsQueryBuilder = new IdsQueryBuilder()
				.types(eventType)
				.addIds(eventId);
		// No principal filtered query. We would like to show the entire event chain, but the user should not be able to retrieve all information.
		SearchResponse response =  client.prepareSearch(ElasticSearchLayout.ETM_EVENT_INDEX_ALIAS_ALL)
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
			result.append("{\"id\": " + escapeToJson(application, true) 
				+ ", \"label\": " + escapeToJson(application, true) 
				+ ", \"node_type\": \"application\""
				+ ", \"missing\": " + eventChain.isApplicationMissing(application) + "}");
			first = false;
		}
		for (EventChainEvent event : eventChain.events.values()) {
			for (EventChainEndpoint endpoint : event.getEndpoints()) {
				// Add all endpoints as item.
				if (endpoint.getName() != null) {
					if (!first) {
						result.append(",");
					}
					result.append("{\"id\": " + escapeToJson(endpoint.getKey(), true) 
						+ ", \"label\": " + escapeToJson(endpoint.getName(), true) 
						+ ", \"event_id\": " + escapeToJson(event.getEventId(), true) 
						+ ", \"event_type\": " + escapeToJson(event.getEventType(), true)
						+ ", \"node_type\": \"endpoint\""
						+ ", \"missing\": " + endpoint.isMissing() + "}");
					first = false;				
				}
				// Add the reader as item.
				if (endpoint.getWriter() != null) {
					if (!first) {
						result.append(",");
					}
					result.append("{\"id\": " + escapeToJson(endpoint.getWriter().getKey(), true) 
						+ ", \"label\": " + escapeToJson(endpoint.getWriter().getName(), true) 
						+ ", \"event_id\": " + escapeToJson(event.getEventId(), true) 
						+ ", \"event_type\": " + escapeToJson(event.getEventType(), true) 
						+ ", \"endpoint\": " + escapeToJson(endpoint.getName(), true) 
						+ ", \"transaction_id\": " + escapeToJson(endpoint.getWriter().getTransactionId(), true) 
						+ ", \"node_type\": \"event\""
						+ ", \"missing\": " + endpoint.getWriter().isMissing());
					if (endpoint.getWriter().getApplicationName() != null) {
						result.append(", \"parent\": " + escapeToJson(endpoint.getWriter().getApplicationName(), true)); 
					}
					result.append("}");
					first = false;			
				}
				// Add all writers as item.
				for (EventChainItem item : endpoint.getReaders()) {
					if (!first) {
						result.append(",");
					}
					result.append("{\"id\": " + escapeToJson(item.getKey(), true) 
						+ ", \"label\": " + escapeToJson(item.getName(), true) 
						+ ", \"event_id\": " + escapeToJson(event.getEventId(), true) 
						+ ", \"event_type\": " + escapeToJson(event.getEventType(), true) 
						+ ", \"endpoint\": " + escapeToJson(endpoint.getName(), true) 
						+ ", \"transaction_id\": " + escapeToJson(item.getTransactionId(), true) 
						+ ", \"node_type\": \"event\""
						+ ", \"missing\": " + item.isMissing());
					if (item.getApplicationName() != null) {
						result.append(", \"parent\": " + escapeToJson(item.getApplicationName(), true)); 
					}
					result.append("}");
					first = false;							
				}
			}
		}
		result.append("], \"edges\": [");
		first = true;
		for (EventChainEvent event : eventChain.events.values()) {
			for (EventChainEndpoint endpoint : event.getEndpoints()) {
				if (endpoint.getName() != null) {
					if (endpoint.getWriter() != null) {
						if (!first) {
							result.append(",");
						}
						// Add the connection from the writer to the endpoint.
						result.append("{\"source\": " + escapeToJson(endpoint.getWriter().getKey(), true) 
								+ ", \"target\": " + escapeToJson(endpoint.getKey(), true));
						if (!endpoint.getWriter().isMissing()) {
							result.append(", \"transition_time_percentage\": 0.0");
						}
						result.append("}");
						first = false;			
					}
					for (EventChainItem item : endpoint.getReaders()) {
						// Add a connection between the endpoint and all readers.
						if (!first) {
							result.append(",");
						}
						result.append("{ \"source\": " +  escapeToJson(endpoint.getKey(), true)
							+ ", \"target\": " + escapeToJson(item.getKey(), true)); 
						Float edgePercentage = eventChain.calculateEdgePercentageFromEndpointToItem(event, endpoint, item);
						if (edgePercentage != null) {
							result.append(", \"transition_time_percentage\": " + edgePercentage);
						}
						result.append("}");
						first = false;							
					}
				} else {
					// No endpoint name, so a direct connection from a writer to the readers.
					if (endpoint.getWriter() != null) {
						for (EventChainItem item : endpoint.getReaders()) {
							// Add a connection between the writer and all readers.
							if (!first) {
								result.append(",");
							}
							result.append("{ \"source\": " +  escapeToJson(endpoint.getWriter().getKey(), true)
								+ ", \"target\": " + escapeToJson(item.getKey(), true)); 
							Float edgePercentage = eventChain.calculateEdgePercentageFromEndpointToItem(event, endpoint, item);
							if (edgePercentage != null) {
								result.append(", \"transition_time_percentage\": " + edgePercentage);
							}						
							result.append("}");
							first = false;							
						}					
					}
				}
			}
			if (event.isRequest()) {
				// If the last part of the request chain is an endpoint (without
				// a reader) and the first part of the request chain has no
				// writers then we lay a connection between the endpoints.
				EventChainEvent responseEvent = eventChain.findResponse(event.getEventId());
				// TODO, add an missing response when responseEvent == null?
				if (responseEvent != null) {
					EventChainEndpoint lastRequestEndpoint = event.getEndpoints().get(event.getEndpoints().size() - 1);
					EventChainEndpoint firstResponseEndpoint = responseEvent.getEndpoints().get(0);
					if (lastRequestEndpoint.getReaders().isEmpty() && firstResponseEndpoint.getWriter() == null) {
						String from = lastRequestEndpoint.getName() != null ? lastRequestEndpoint.getKey() : lastRequestEndpoint.getWriter().getKey();
						String to = firstResponseEndpoint.getName() != null ? firstResponseEndpoint.getKey() : firstResponseEndpoint.getReaders().get(0).getKey();
						if (!first) {
							result.append(",");
						}
						result.append("{ \"source\": " +  escapeToJson(from, true)
							+ ", \"target\": " + escapeToJson(to, true)); 
						Float edgePercentage = eventChain.calculateEdgePercentageFromItemToItem(lastRequestEndpoint.getWriter(), firstResponseEndpoint.getReaders().get(0));
						if (edgePercentage != null) {
							result.append(", \"transition_time_percentage\": " + edgePercentage);
						}
						result.append("}");
						first = false;													
					}
				}
			}
			// TODO, achmea maatwerk om de dispatchers te koppelen aan de flows daarna.
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
				.minimumShouldMatch(1)
				.should(new TermQueryBuilder(this.eventTags.getEndpointsTag() + 
						"." + this.eventTags.getReadingEndpointHandlersTag() + 
						"." + this.eventTags.getEndpointHandlerTransactionIdTag(), transactionId))
				.should(new TermQueryBuilder(this.eventTags.getEndpointsTag() + 
						"." + this.eventTags.getWritingEndpointHandlerTag() + 
						"." + this.eventTags.getEndpointHandlerTransactionIdTag(), transactionId));
		// No principal filtered query. We would like to show the entire event chain, but the user should not be able to retrieve all information.
		SearchRequestBuilder searchRequest = client.prepareSearch(ElasticSearchLayout.ETM_EVENT_INDEX_ALIAS_ALL)
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
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequest);
		if (!scrollableSearch.hasNext()) {
			scrollableSearch.clearScrollIds();
			return;
		}
		for (SearchHit searchHit : scrollableSearch) {
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
		scrollableSearch.clearScrollIds();
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
				queryBuilder = new IdsQueryBuilder().types("messaging").addIds(correlationId);
			}
		} else if ("http".equals("type")) {
			HttpEventType httpEventType = HttpEventType.safeValueOf(subType);
			if (HttpEventType.RESPONSE.equals(httpEventType) && correlationId != null) {
				queryBuilder = new IdsQueryBuilder().types("http").addIds(correlationId);
			} else {
				queryBuilder = new BoolQueryBuilder().must(new TermQueryBuilder(this.eventTags.getCorrelationIdTag(), id));
			}
		}
		if (queryBuilder == null) {
			return;
		}
		SearchResponse response =  client.prepareSearch(ElasticSearchLayout.ETM_EVENT_INDEX_ALIAS_ALL)
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
	

}
