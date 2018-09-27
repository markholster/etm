package com.jecstar.etm.gui.rest.services.search;

import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.HttpTelemetryEvent.HttpEventType;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.gui.rest.export.FileType;
import com.jecstar.etm.gui.rest.export.QueryExporter;
import com.jecstar.etm.gui.rest.services.AbstractIndexMetadataService;
import com.jecstar.etm.gui.rest.services.Keyword;
import com.jecstar.etm.gui.rest.services.search.eventchain.*;
import com.jecstar.etm.server.core.domain.audit.AuditLog;
import com.jecstar.etm.server.core.domain.audit.GetEventAuditLog;
import com.jecstar.etm.server.core.domain.audit.builder.GetEventAuditLogBuilder;
import com.jecstar.etm.server.core.domain.audit.builder.QueryAuditLogBuilder;
import com.jecstar.etm.server.core.domain.audit.converter.GetEventAuditLogConverter;
import com.jecstar.etm.server.core.domain.audit.converter.QueryAuditLogConverter;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.configuration.converter.EtmConfigurationTags;
import com.jecstar.etm.server.core.domain.configuration.converter.json.EtmConfigurationTagsJsonImpl;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import com.jecstar.etm.server.core.util.DateUtils;
import com.jecstar.etm.server.core.util.LegacyEndpointHandler;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTimeZone;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Path("/search")
@DeclareRoles(SecurityRoles.ALL_ROLES)
public class SearchService extends AbstractIndexMetadataService {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(SearchService.class);

    private static final DateTimeFormatter dateTimeFormatterIndexPerDay = DateUtils.getIndexPerDayFormatter();
    private static Client client;
    private static EtmConfiguration etmConfiguration;

    private final TelemetryEventTags eventTags = new TelemetryEventTagsJsonImpl();
    private final EtmConfigurationTags configurationTags = new EtmConfigurationTagsJsonImpl();
    private final QueryAuditLogConverter queryAuditLogConverter = new QueryAuditLogConverter();
    private final GetEventAuditLogConverter getEventAuditLogConverter = new GetEventAuditLogConverter();

    public static void initialize(Client client, EtmConfiguration etmConfiguration) {
        SearchService.client = client;
        SearchService.etmConfiguration = etmConfiguration;
    }

    @GET
    @Path("/templates")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String getSearchTemplates() {
        EtmPrincipal etmPrincipal = getEtmPrincipal();
        GetResponse getResponse = client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + etmPrincipal.getId())
                .setFetchSource(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + ".search_templates", null)
                .get();
        if (getResponse.isSourceEmpty() || getResponse.getSourceAsMap().isEmpty() || !getResponse.getSourceAsMap().containsKey(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER)) {
            return "{\"max_search_templates\": " + etmConfiguration.getMaxSearchTemplateCount() + ", \"default_search_range\": " + etmPrincipal.getDefaultSearchRange() + "}";
        }
        Map<String, Object> valueMap = getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, getResponse.getSourceAsMap(), Collections.emptyMap());
        valueMap.put("max_search_templates", etmConfiguration.getMaxSearchTemplateCount());
        valueMap.put("default_search_range", etmPrincipal.getDefaultSearchRange());
        return toString(valueMap);
    }

    @GET
    @Path("/history")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String getRecentQueries() {
        EtmPrincipal etmPrincipal = getEtmPrincipal();
        GetResponse getResponse = client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + etmPrincipal.getId())
                .setFetchSource(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.configurationTags.getSearchHistoryTag(), null)
                .get();
        if (getResponse.isSourceEmpty() || !getResponse.getSourceAsMap().containsKey(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER)) {
            return "{}";
        }
        Map<String, Object> valueMap = getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, getResponse.getSourceAsMap());
        return toString(valueMap);
    }

    @PUT
    @Path("/templates/{templateName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String addSearchTemplate(@PathParam("templateName") String templateName, String json) {
        Map<String, Object> requestValues = toMap(json);
        Map<String, Object> scriptParams = new HashMap<>();
        Map<String, Object> template = new HashMap<>();
        template.put("name", templateName);
        template.put("query", getString("query", requestValues));
        template.put("types", getArray("types", requestValues));
        template.put("fields", getArray("fields", requestValues));
        template.put("results_per_page", getInteger("results_per_page", requestValues, 50));
        template.put("sort_field", getString("sort_field", requestValues));
        template.put("sort_order", getString("sort_order", requestValues));
        template.put("start_time", getString("start_time", requestValues));
        template.put("end_time", getString("end_time", requestValues));
        template.put("time_filter_field", getString("time_filter_field", requestValues));

        scriptParams.put("template", template);
        scriptParams.put("max_templates", etmConfiguration.getMaxSearchTemplateCount());
        enhanceRequest(
                client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + getEtmPrincipal().getId()),
                etmConfiguration
        )
                .setScript(new Script(ScriptType.STORED, null, "etm_update-search-template", scriptParams))
                .get();
        return "{ \"status\": \"success\" }";
    }

    @DELETE
    @Path("/templates/{templateName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String removeSearchTemplate(@PathParam("templateName") String templateName) {
        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put("name", templateName);
        enhanceRequest(
                client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + getEtmPrincipal().getId()),
                etmConfiguration
        )
                .setScript(new Script(ScriptType.STORED, null, "etm_remove-search-template", scriptParams))
                .get();
        return "{ \"status\": \"success\" }";
    }

    @GET
    @Path("/keywords/{indexName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String getKeywords(@PathParam("indexName") String indexName) {
        StringBuilder result = new StringBuilder();
        Map<String, List<Keyword>> names = getIndexFields(SearchService.client, indexName);
        result.append("{ \"keywords\":[");
        Set<Entry<String, List<Keyword>>> entries = names.entrySet();
        boolean first = true;
        for (Entry<String, List<Keyword>> entry : entries) {
            if (!first) {
                result.append(", ");
            }
            first = false;
            result.append("{");
            result.append("\"index\": ").append(escapeToJson(indexName, true)).append(",");
            result.append("\"keywords\": [").append(entry.getValue().stream().map(n ->
            {
                StringBuilder kw = new StringBuilder();
                kw.append("{");
                addStringElementToJsonBuffer("name", n.getName(), kw, true);
                addStringElementToJsonBuffer("type", n.getType(), kw, false);
                addBooleanElementToJsonBuffer("date", n.isDate(), kw, false);
                addBooleanElementToJsonBuffer("number", n.isNumber(), kw, false);
                kw.append("}");
                return kw.toString();
            }).collect(Collectors.joining(", "))).append("]");
            result.append("}");
        }
        result.append("]}");
        return result.toString();
    }

    @POST
    @Path("/query")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String executeQuery(String json) {
        long startTime = System.currentTimeMillis();
        EtmPrincipal etmPrincipal = getEtmPrincipal();

        ZonedDateTime now = ZonedDateTime.now();
        QueryAuditLogBuilder auditLogBuilder = new QueryAuditLogBuilder().setTimestamp(now).setHandlingTime(now).setPrincipalId(etmPrincipal.getId());

        SearchRequestParameters parameters = new SearchRequestParameters(toMap(json), etmPrincipal);
        SearchRequestBuilder requestBuilder = createRequestFromInput(parameters, etmPrincipal);
        NumberFormat numberFormat = NumberFormat.getInstance(etmPrincipal.getLocale());
        SearchResponse response = requestBuilder.get();
        StringBuilder result = new StringBuilder();
        result.append("{");
        result.append("\"status\": \"success\"");
        result.append(",\"history_size\": ").append(etmPrincipal.getHistorySize());
        result.append(",\"hits\": ").append(response.getHits().getTotalHits());
        result.append(",\"hits_as_string\": \"").append(numberFormat.format(response.getHits().getTotalHits())).append("\"");
        result.append(",\"time_zone\": \"").append(etmPrincipal.getTimeZone().getID()).append("\"");
        result.append(",\"start_ix\": ").append(parameters.getStartIndex());
        result.append(",\"end_ix\": ").append(parameters.getStartIndex() + response.getHits().getHits().length - 1);
        result.append(",\"has_more_results\": ").append(parameters.getStartIndex() + response.getHits().getHits().length < response.getHits().getTotalHits() - 1);
        result.append(",\"time_zone\": \"").append(etmPrincipal.getTimeZone().getID()).append("\"");
        result.append(",\"max_downloads\": ").append(etmConfiguration.getMaxSearchResultDownloadRows());
        result.append(",\"may_see_payload\": ").append(etmPrincipal.isInAnyRole(SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WRITE));
        result.append(",\"results\": [");
        addSearchHits(result, response.getHits());
        result.append("]");
        long queryTime = System.currentTimeMillis() - startTime;
        result.append(",\"query_time\": ").append(queryTime);
        result.append(",\"query_time_as_string\": \"").append(numberFormat.format(queryTime)).append("\"");
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
                executedQuery = Strings.toString(contentBuilder);
            } catch (IOException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage(e.getMessage(), e);
                }
            }
            auditLogBuilder
                    .setUserQuery(parameters.getQueryString())
                    .setExectuedQuery(executedQuery)
                    .setNumberOfResults(response.getHits().getTotalHits())
                    .setQueryTime(queryTime);
            enhanceRequest(
                    client.prepareIndex(ElasticsearchLayout.AUDIT_LOG_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(now), ElasticsearchLayout.ETM_DEFAULT_TYPE),
                    etmConfiguration
            )
                    .setSource(this.queryAuditLogConverter.write(auditLogBuilder.build()), XContentType.JSON)
                    .execute();
        }
        return result.toString();
    }

    private SearchRequestBuilder createRequestFromInput(SearchRequestParameters parameters, EtmPrincipal etmPrincipal) {
        if (!parameters.hasFields()) {
            parameters.addField(this.eventTags.getEndpointsTag() + "." + this.eventTags.getEndpointHandlersTag() + "." + this.eventTags.getEndpointHandlerHandlingTimeTag());
            parameters.addField(this.eventTags.getNameTag());
        }
        QueryStringQueryBuilder queryStringBuilder = new QueryStringQueryBuilder(parameters.getQueryString())
                .allowLeadingWildcard(true)
                .analyzeWildcard(true)
                .defaultField(ElasticsearchLayout.ETM_ALL_FIELDS_ATTRIBUTE_NAME)
                .timeZone(DateTimeZone.forTimeZone(etmPrincipal.getTimeZone()));
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(queryStringBuilder);


        boolean notAfterFilterNecessary = true;
        if (parameters.getEndTime() != null || parameters.getStartTime() != null) {
            RangeQueryBuilder timestampFilter = new RangeQueryBuilder(parameters.getTimeFilterField());
            if (parameters.getEndTime() != null) {
                try {
                    // Check if the endtime is given as an exact timestamp or an elasticsearch date math.
                    long endTime = Long.valueOf(parameters.getEndTime());
                    timestampFilter.lte(endTime < parameters.getNotAfterTimestamp() ? endTime : parameters.getNotAfterTimestamp());
                    notAfterFilterNecessary = false;
                } catch (NumberFormatException e) {
                    // Endtime was an elasticsearch date math. Check if it is the exact value of "now"
                    if ("now" .equalsIgnoreCase(parameters.getEndTime())) {
                        // Replace "now" with the notAfter timestamp which is in essence the same
                        timestampFilter.lte(parameters.getNotAfterTimestamp());
                        notAfterFilterNecessary = false;
                    } else {
                        timestampFilter.lte(parameters.getEndTime());
                    }
                }
            } else {
                timestampFilter.lte(parameters.getNotAfterTimestamp());
            }
            if (parameters.getStartTime() != null) {
                try {
                    // Check if the starttime is given as an exact timestamp or an elasticsearch date math.
                    long endTime = Long.valueOf(parameters.getStartTime());
                    timestampFilter.gte(endTime);
                } catch (NumberFormatException e) {
                    timestampFilter.gte(parameters.getStartTime());
                }
            }
            boolQueryBuilder.filter(timestampFilter);
        }
        if (notAfterFilterNecessary) {
            // the given parameters.getStartTime() & parameters.getEndTime() were zero or an elasticsearch math date. We have to apply the notAfterTime filter as well.
            boolQueryBuilder.filter(new RangeQueryBuilder("timestamp").lte(parameters.getNotAfterTimestamp()));
        }
        if (parameters.getTypes().size() != 5) {
            if (ElasticsearchLayout.OLD_EVENT_TYPES_PRESENT) {
                boolQueryBuilder.filter(QueryBuilders.boolQuery()
                        .should(QueryBuilders.termsQuery("_type", parameters.getTypes().toArray()))
                        .should(QueryBuilders.boolQuery()
                                .must(QueryBuilders.termQuery("_type", ElasticsearchLayout.ETM_DEFAULT_TYPE))
                                .must(QueryBuilders.termsQuery(this.eventTags.getObjectTypeTag(), parameters.getTypes().toArray())))
                        .minimumShouldMatch(1));
            } else {
                boolQueryBuilder.filter(QueryBuilders.termsQuery(this.eventTags.getObjectTypeTag(), parameters.getTypes().toArray()));
            }
        }
        SearchRequestBuilder requestBuilder = enhanceRequest(client.prepareSearch(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL), etmConfiguration)
                .setQuery(addFilterQuery(getEtmPrincipal(), boolQueryBuilder))
                .setFetchSource(parameters.getFields().toArray(new String[parameters.getFields().size()]), null)
                .setFrom(parameters.getStartIndex())
                .setSize(parameters.getMaxResults() > 500 ? 500 : parameters.getMaxResults());
        if (parameters.getSortField() != null && parameters.getSortField().trim().length() > 0) {
            String sortProperty = getSortProperty(client, ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL, parameters.getSortField());
            if (sortProperty != null) {
                requestBuilder.addSort(sortProperty, "desc".equals(parameters.getSortOrder()) ? SortOrder.DESC : SortOrder.ASC);
            }
        }
        return requestBuilder;
    }

    private void writeQueryHistory(long timestamp, SearchRequestParameters parameters, EtmPrincipal etmPrincipal, int history_size) {
        Map<String, Object> scriptParams = new HashMap<>();
        Map<String, Object> query = new HashMap<>();
        query.put(this.configurationTags.getTimestampTag(), timestamp);
        query.put(this.configurationTags.getQueryTag(), parameters.getQueryString());
        query.put(this.configurationTags.getTypesTag(), parameters.getTypes());
        query.put(this.configurationTags.getFieldsTag(), parameters.getFieldsLayout());
        query.put(this.configurationTags.getResultsPerPageTag(), parameters.getMaxResults());
        query.put(this.configurationTags.getSortFieldTag(), parameters.getSortField());
        query.put(this.configurationTags.getSortOrderTag(), parameters.getSortOrder());
        query.put(this.configurationTags.getStartTimeTag(), parameters.getStartTime());
        query.put(this.configurationTags.getEndTimeTag(), parameters.getEndTime());
        query.put(this.configurationTags.getTimeFilterFieldTag(), parameters.getTimeFilterField());


        scriptParams.put("query", query);
        scriptParams.put("history_size", history_size);
        enhanceRequest(
                client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + etmPrincipal.getId()),
                etmConfiguration
        )
                .setScript(new Script(ScriptType.STORED, null, "etm_update-search-history", scriptParams))
                .execute();
    }


    @GET
    @Path("/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public Response getDownload(@QueryParam("q") String json) {
        EtmPrincipal etmPrincipal = getEtmPrincipal();
        Map<String, Object> valueMap = toMap(json);
        SearchRequestParameters parameters = new SearchRequestParameters(valueMap, etmPrincipal);
        if (getBoolean("includePayload", valueMap)
                && !parameters.getFields().contains(this.eventTags.getPayloadTag())) {
            parameters.addField(this.eventTags.getPayloadTag());
            Map<String, Object> payloadFieldLayout = new HashMap<>();
            payloadFieldLayout.put("name", "Payload");
            payloadFieldLayout.put("field", "payload");
            payloadFieldLayout.put("format", "plain");
            payloadFieldLayout.put("array", "first");
            parameters.addFieldLayout(payloadFieldLayout);

        }
        SearchRequestBuilder requestBuilder = createRequestFromInput(parameters, etmPrincipal);

        ScrollableSearch scrollableSearch = new ScrollableSearch(client, requestBuilder, parameters.getStartIndex());
        FileType fileType = FileType.valueOf(getString("fileType", valueMap).toUpperCase());
        File result = new QueryExporter().exportToFile(scrollableSearch, fileType, Math.min(parameters.getMaxResults(), etmConfiguration.getMaxSearchResultDownloadRows()), etmPrincipal, parameters.toFieldLayouts());
        ResponseBuilder response = Response.ok(result);
        response.header("Content-Disposition", "attachment; filename=etm-results." + fileType.name().toLowerCase());
        response.encoding(System.getProperty("file.encoding"));
        response.header("Content-Type", fileType.getContentType());
        return response.build();
    }

    @GET
    @Path("/event/{type}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String getEvent(@PathParam("type") String eventType, @PathParam("id") String eventId) {
        ZonedDateTime now = ZonedDateTime.now();
        GetEventAuditLogBuilder auditLogBuilder = new GetEventAuditLogBuilder()
                .setTimestamp(now)
                .setHandlingTime(now)
                .setPrincipalId(getEtmPrincipal().getId())
                .setEventId(eventId)
                .setEventType(eventType)
                .setFound(false);
        ActionFuture<SearchResponse> auditLogsForEvent = null;
        if (getEtmPrincipal().isInRole(SecurityRoles.AUDIT_LOG_READ)) {
            // We've got an principal with audit logs roles requesting the event. Also add the audit logs of this event to the response.
            auditLogsForEvent = findAuditLogsForEvent(eventType, eventId);
        }
        StringBuilder result = new StringBuilder();
        result.append("{");
        addStringElementToJsonBuffer("time_zone", getEtmPrincipal().getTimeZone().getID(), result, true);
        SearchHit searchHit = getEvent(eventType, eventId, new String[]{"*"}, getEtmPrincipal().isInRole(SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD) ? new String[]{this.eventTags.getPayloadTag()} : null);
        if (searchHit != null) {
            auditLogBuilder.setFound(true);
            Map<String, Object> valueMap = searchHit.getSourceAsMap();
            replaceLegacyEndpointHandlerLocations(valueMap);
            result.append(", \"event\": {");
            addStringElementToJsonBuffer("index", searchHit.getIndex(), result, true);
            addStringElementToJsonBuffer("type", searchHit.getType(), result, false);
            addStringElementToJsonBuffer("id", searchHit.getId(), result, false);
            result.append(", \"source\": ").append(toString(valueMap));
            result.append("}");
            // Add the name to the audit log.
            auditLogBuilder.setEventName(getString(this.eventTags.getNameTag(), valueMap));
            // Try to find an event this event is correlating to.
            String correlatedToId = getString(this.eventTags.getCorrelationIdTag(), valueMap);
            boolean correlationAdded = false;
            if (correlatedToId != null && !correlatedToId.equals(eventId)) {
                SearchHit correlatedEvent = conditionallyGetEvent(eventType, correlatedToId);
                if (correlatedEvent != null) {
                    Map<String, Object> sourceAsMap = correlatedEvent.getSourceAsMap();
                    // TODO meteen sourceAsString op het moment dat onderstaande methode wordt verwijderd.
                    replaceLegacyEndpointHandlerLocations(sourceAsMap);
                    result.append(", \"correlated_events\": [");
                    result.append("{");
                    addStringElementToJsonBuffer("index", correlatedEvent.getIndex(), result, true);
                    addStringElementToJsonBuffer("type", correlatedEvent.getType(), result, false);
                    addStringElementToJsonBuffer("id", correlatedEvent.getId(), result, false);
                    result.append(", \"source\": ").append(toString(sourceAsMap));
                    result.append("}");
                    correlationAdded = true;
                    auditLogBuilder.addCorrelatedEvent(correlatedEvent.getId(), correlatedEvent.getType());
                }
            }
            // Try to find event that correlate to this event.
            List<String> correlations = getArray(this.eventTags.getCorrelationsTag(), valueMap);
            if (correlations != null && !correlations.isEmpty()) {
                int added = 0;
                for (int i = 0; i < correlations.size() && added <= 10; i++) {
                    String correlationId = correlations.get(i);
                    if (correlationId == null) {
                        continue;
                    }
                    if (eventId.equals(correlationId)) {
                        // An event correlates to itself.
                        continue;
                    }
                    SearchHit correlatedEvent = conditionallyGetEvent(eventType, correlationId);
                    if (correlatedEvent != null) {
                        Map<String, Object> sourceAsMap = correlatedEvent.getSourceAsMap();
                        // TODO meteen sourceAsString op het moment dat onderstaande methode wordt verwijderd.
                        replaceLegacyEndpointHandlerLocations(sourceAsMap);
                        added++;
                        if (!correlationAdded) {
                            result.append(", \"correlated_events\": [");
                        } else {
                            result.append(",");
                        }
                        result.append("{");
                        addStringElementToJsonBuffer("index", correlatedEvent.getIndex(), result, true);
                        addStringElementToJsonBuffer("type", correlatedEvent.getType(), result, false);
                        addStringElementToJsonBuffer("id", correlatedEvent.getId(), result, false);
                        result.append(", \"source\": ").append(toString(sourceAsMap));
                        result.append("}");
                        correlationAdded = true;
                        auditLogBuilder.addCorrelatedEvent(correlatedEvent.getId(), correlatedEvent.getType());
                    }
                }
            }
            if (correlationAdded) {
                result.append("]");
            }
            if (auditLogsForEvent != null) {
                // Audit logs received async. Wait here to add the to the result.
                SearchResponse auditLogResponse = auditLogsForEvent.actionGet();
                if (auditLogResponse.getHits().getHits().length != 0) {
                    boolean auditLogAdded = false;
                    result.append(", \"audit_logs\": [");
                    for (SearchHit hit : auditLogResponse.getHits().getHits()) {
                        Map<String, Object> auditLogValues = hit.getSourceAsMap();
                        if (auditLogAdded) {
                            result.append(",");
                        } else {
                            auditLogAdded = true;
                        }
                        result.append("{");
                        addBooleanElementToJsonBuffer("direct", getString(GetEventAuditLog.EVENT_ID, auditLogValues).equals(eventId), result, true);
                        addLongElementToJsonBuffer("handling_time", getLong(AuditLog.HANDLING_TIME, auditLogValues), result, false);
                        addStringElementToJsonBuffer("principal_id", getString(AuditLog.PRINCIPAL_ID, auditLogValues), result, false);
                        result.append("}");
                    }
                    result.append("]");
                }
            }

        }
        result.append("}");
        // Log the retrieval request to the audit logs.
        enhanceRequest(
                client.prepareIndex(ElasticsearchLayout.AUDIT_LOG_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(now), ElasticsearchLayout.ETM_DEFAULT_TYPE),
                etmConfiguration
        )
                .setSource(this.getEventAuditLogConverter.write(auditLogBuilder.build()), XContentType.JSON)
                .execute();
        return result.toString();
    }

    @LegacyEndpointHandler("Remove method")
    private void replaceLegacyEndpointHandlerLocations(Map<String, Object> sourceMap) {
        List<Map<String, Object>> endpointsMap = getArray(this.eventTags.getEndpointsTag(), sourceMap);
        if (endpointsMap == null) {
            return;
        }
        for (Map<String, Object> endpointMap : endpointsMap) {
            List<Map<String, Object>> endpointHandlers = new ArrayList<>();
            Map<String, Object> writingEndpointHandler = getObject("writing_endpoint_handler", endpointMap, null);
            if (writingEndpointHandler != null) {
                writingEndpointHandler.put(this.eventTags.getEndpointHandlerTypeTag(), EndpointHandler.EndpointHandlerType.WRITER.name());
                endpointHandlers.add(writingEndpointHandler);
                endpointMap.remove("writing_endpoint_handler");
            }
            List<Map<String, Object>> readingEndpointHandlers = getArray("reading_endpoint_handlers", endpointMap);
            if (readingEndpointHandlers != null) {
                for (Map<String, Object> readingEndpointHandler : readingEndpointHandlers) {
                    readingEndpointHandler.put(this.eventTags.getEndpointHandlerTypeTag(), EndpointHandler.EndpointHandlerType.READER.name());
                    endpointHandlers.add(readingEndpointHandler);
                }
                endpointMap.remove("reading_endpoint_handlers");
            }
            if (endpointHandlers.size() > 0) {
                endpointMap.put(this.eventTags.getEndpointHandlersTag(), endpointHandlers);
            }
        }
    }

    private ActionFuture<SearchResponse> findAuditLogsForEvent(String eventType, String eventId) {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(new TermQueryBuilder(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.AUDIT_LOG_OBJECT_TYPE_GET_EVENT));
        boolQueryBuilder.must(new TermQueryBuilder(GetEventAuditLog.EVENT_TYPE + KEYWORD_SUFFIX, eventType));
        boolQueryBuilder.should(new TermQueryBuilder(GetEventAuditLog.EVENT_ID + KEYWORD_SUFFIX, eventId));
        boolQueryBuilder.should(new TermQueryBuilder(GetEventAuditLog.CORRELATED_EVENTS + "." + GetEventAuditLog.EVENT_ID + KEYWORD_SUFFIX, eventId));
        boolQueryBuilder.minimumShouldMatch(1);
        SearchRequestBuilder requestBuilder = enhanceRequest(client.prepareSearch(ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL), etmConfiguration)
                .setQuery(boolQueryBuilder)
                .addSort(AuditLog.HANDLING_TIME, SortOrder.DESC)
                .setFetchSource(new String[]{AuditLog.HANDLING_TIME, AuditLog.PRINCIPAL_ID, GetEventAuditLog.EVENT_ID}, null)
                .setFrom(0)
                .setSize(500);
        return requestBuilder.execute();
    }

    @GET
    @Path("/event/{id}/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String getEventChainEndpoint(@PathParam("id") String eventId) {
        StringBuilder result = new StringBuilder();
        result.append("{");
        addStringElementToJsonBuffer("time_zone", getEtmPrincipal().getTimeZone().getID(), result, true);
        SearchHit searchHit = getEvent(null, eventId, null, new String[]{this.eventTags.getEndpointsTag() + ".*"});
        if (searchHit != null) {
            result.append(", \"event\": {");
            addStringElementToJsonBuffer("index", searchHit.getIndex(), result, true);
            addStringElementToJsonBuffer("type", searchHit.getType(), result, false);
            addStringElementToJsonBuffer("id", searchHit.getId(), result, false);
            result.append(", \"source\": ").append(searchHit.getSourceAsString());
            result.append("}");
        }
        result.append("}");
        return result.toString();
    }

    private SearchHit getEvent(String eventType, String eventId, String[] includes, String[] excludes) {
        IdsQueryBuilder idsQueryBuilder = new IdsQueryBuilder()
                .addIds(eventId);
        if (eventType != null) {
            idsQueryBuilder.types(eventType);
        }
        BoolQueryBuilder query = new BoolQueryBuilder();
        query.must(idsQueryBuilder);
        SearchRequestBuilder builder = enhanceRequest(client.prepareSearch(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL), etmConfiguration)
                .setQuery(addFilterQuery(getEtmPrincipal(), query))
                .setFrom(0)
                .setSize(1);
        builder.setFetchSource(includes, excludes);
        SearchResponse response = builder.get();
        if (response.getHits().getHits().length == 0) {
            return null;
        }
        return response.getHits().getAt(0);
    }

    private SearchHit conditionallyGetEvent(String eventType, String eventId) {
        IdsQueryBuilder idsQueryBuilder = new IdsQueryBuilder()
                .types(eventType)
                .addIds(eventId);
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder().must(idsQueryBuilder);
        SearchRequestBuilder builder = enhanceRequest(client.prepareSearch(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL), etmConfiguration)
                .setQuery(alwaysShowCorrelatedEvents(getEtmPrincipal()) ? idsQueryBuilder : addFilterQuery(getEtmPrincipal(), boolQueryBuilder))
                .setFrom(0)
                .setSize(1)
                .setFetchSource(true);
        SearchResponse response = builder.get();
        if (response.getHits().getHits().length == 0) {
            return null;
        }
        return response.getHits().getAt(0);
    }

    private boolean alwaysShowCorrelatedEvents(EtmPrincipal etmPrincipal) {
        if (!hasFilterQueries(getEtmPrincipal())) {
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
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String getTransaction(@PathParam("application") String applicationName, @PathParam("id") String transactionId) {
        List<TransactionEvent> events = getTransactionEvents(applicationName, transactionId);
        if (events == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        result.append("{");
        addStringElementToJsonBuffer("time_zone", getEtmPrincipal().getTimeZone().getID(), result, true);
        result.append(",\"events\":[");
        boolean first = true;
        for (TransactionEvent event : events) {
            if (first) {
                result.append("{");
                first = false;
            } else {
                result.append(", {");
            }
            addStringElementToJsonBuffer("index", event.index, result, true);
            addStringElementToJsonBuffer("type", event.type, result, false);
            addStringElementToJsonBuffer("object_type", event.objectType, result, false);
            addStringElementToJsonBuffer("sub_type", event.subtype, result, false);
            addStringElementToJsonBuffer("id", event.id, result, false);
            addLongElementToJsonBuffer("handling_time", event.handlingTime, result, false);
            addStringElementToJsonBuffer("name", event.name, result, false);
            addStringElementToJsonBuffer("direction", event.direction, result, false);
            addStringElementToJsonBuffer("payload", event.payload, result, false);
            addStringElementToJsonBuffer("endpoint", event.endpoint, result, false);
            result.append("}");
        }
        result.append("]}");
        return result.toString();
    }

    @LegacyEndpointHandler("findEventsQuery must be changed to work with a single must instead of 3 shoulds.")
    private List<TransactionEvent> getTransactionEvents(String applicationName, String transactionId) {
        BoolQueryBuilder findEventsQuery = new BoolQueryBuilder()
                .minimumShouldMatch(1)
                .should(
                        new BoolQueryBuilder()
                                .must(new TermQueryBuilder(this.eventTags.getEndpointsTag() +
                                        "." + this.eventTags.getEndpointHandlersTag() +
                                        "." + this.eventTags.getEndpointHandlerApplicationTag() +
                                        "." + this.eventTags.getApplicationNameTag() + KEYWORD_SUFFIX, applicationName))
                                .must(new TermQueryBuilder(this.eventTags.getEndpointsTag() +
                                        "." + this.eventTags.getEndpointHandlersTag() +
                                        "." + this.eventTags.getEndpointHandlerTransactionIdTag() + KEYWORD_SUFFIX, transactionId))
                ).should(
                        new BoolQueryBuilder()
                                .must(new TermQueryBuilder(this.eventTags.getEndpointsTag() +
                                        ".reading_endpoint_handlers" +
                                        "." + this.eventTags.getEndpointHandlerApplicationTag() +
                                        "." + this.eventTags.getApplicationNameTag() + KEYWORD_SUFFIX, applicationName))
                                .must(new TermQueryBuilder(this.eventTags.getEndpointsTag() +
                                        ".reading_endpoint_handlers" +
                                        "." + this.eventTags.getEndpointHandlerTransactionIdTag() + KEYWORD_SUFFIX, transactionId))
                ).should(
                        new BoolQueryBuilder()
                                .must(new TermQueryBuilder(this.eventTags.getEndpointsTag() +
                                        ".writing_endpoint_handler" +
                                        "." + this.eventTags.getEndpointHandlerApplicationTag() +
                                        "." + this.eventTags.getApplicationNameTag() + KEYWORD_SUFFIX, applicationName))
                                .must(new TermQueryBuilder(this.eventTags.getEndpointsTag() +
                                        ".writing_endpoint_handler" +
                                        "." + this.eventTags.getEndpointHandlerTransactionIdTag() + KEYWORD_SUFFIX, transactionId))
                );
        SearchRequestBuilder searchRequest = enhanceRequest(client.prepareSearch(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL), etmConfiguration)
                .setQuery(addFilterQuery(getEtmPrincipal(), findEventsQuery))
                .addSort(SortBuilders.fieldSort("_doc"))
                .setFetchSource(new String[]{
                        this.eventTags.getObjectTypeTag(),
                        this.eventTags.getEndpointsTag() + ".*",
                        this.eventTags.getNameTag(),
                        this.eventTags.getPayloadTag(),
                        this.eventTags.getMessagingEventTypeTag(),
                        this.eventTags.getHttpEventTypeTag(),
                        this.eventTags.getSqlEventTypeTag()}, null
                );
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequest);
        if (!scrollableSearch.hasNext()) {
            scrollableSearch.clearScrollIds();
            return null;
        }
        List<TransactionEvent> events = new ArrayList<>();
        for (SearchHit searchHit : scrollableSearch) {
            TransactionEvent event = new TransactionEvent();
            event.index = searchHit.getIndex();
            event.type = searchHit.getType();
            event.objectType = getEventType(searchHit);
            event.id = searchHit.getId();
            Map<String, Object> source = searchHit.getSourceAsMap();
            event.name = getString(this.eventTags.getNameTag(), source);
            event.payload = getString(this.eventTags.getPayloadTag(), source);
            List<Map<String, Object>> endpoints = getArray(this.eventTags.getEndpointsTag(), source);
            if (endpoints != null) {
                for (Map<String, Object> endpoint : endpoints) {
                    List<Map<String, Object>> endpointHandlers = getArray(this.eventTags.getEndpointHandlersTag(), endpoint);
                    if (endpointHandlers != null) {
                        for (Map<String, Object> eh : endpointHandlers) {
                            if (isWithinTransaction(eh, applicationName, transactionId)) {
                                event.handlingTime = getLong(this.eventTags.getEndpointHandlerHandlingTimeTag(), eh);
                                event.direction = EndpointHandler.EndpointHandlerType.WRITER.name().equals(getString(this.eventTags.getEndpointHandlerTypeTag(), eh)) ? "outgoing" : "incoming";
                                event.endpoint = getString(this.eventTags.getEndpointNameTag(), endpoint);
                                event.sequenceNumber = getInteger(this.eventTags.getEndpointHandlerSequenceNumberTag(), eh);
                            }
                        }
                    } else {
                        addLegacyHandlersToTransaction(event, applicationName, transactionId, endpoint);
                    }
                }
            }
            if ("http".equals(event.objectType)) {
                event.subtype = getString(this.eventTags.getHttpEventTypeTag(), source);
            } else if ("messaging".equals(event.objectType)) {
                event.subtype = getString(this.eventTags.getMessagingEventTypeTag(), source);
            } else if ("sql".equals(event.objectType)) {
                event.subtype = getString(this.eventTags.getSqlEventTypeTag(), source);
            }

            if (event.sequenceNumber == null) {
                // Make sure the sequence number is always filled because otherwise the compare on sequence will fail.
                event.sequenceNumber = Integer.MAX_VALUE;
            }
            events.add(event);
        }
        events.sort(Comparator.comparing((TransactionEvent e) -> e.handlingTime).thenComparing(e -> e.sequenceNumber));
        return events;
    }

    @LegacyEndpointHandler("Method must be removed")
    private void addLegacyHandlersToTransaction(TransactionEvent event, String applicationName, String transactionId, Map<String, Object> endpoint) {
        Map<String, Object> writingEndpointHandler = getObject("writing_endpoint_handler", endpoint);
        if (isWithinTransaction(writingEndpointHandler, applicationName, transactionId)) {
            event.handlingTime = getLong(this.eventTags.getEndpointHandlerHandlingTimeTag(), writingEndpointHandler);
            event.direction = "outgoing";
            event.endpoint = getString(this.eventTags.getEndpointNameTag(), endpoint);
            Integer sequenceNumber = getInteger(this.eventTags.getEndpointHandlerSequenceNumberTag(), writingEndpointHandler);
            if (sequenceNumber != null && sequenceNumber.longValue() < event.sequenceNumber.longValue()) {
                event.sequenceNumber = sequenceNumber;
            }
        } else {
            List<Map<String, Object>> readingEndpointHandlers = getArray("reading_endpoint_handlers", endpoint);
            if (readingEndpointHandlers != null) {
                for (Map<String, Object> readingEndpointHandler : readingEndpointHandlers) {
                    if (isWithinTransaction(readingEndpointHandler, applicationName, transactionId)) {
                        event.handlingTime = getLong(this.eventTags.getEndpointHandlerHandlingTimeTag(), readingEndpointHandler);
                        event.direction = "incoming";
                        event.endpoint = getString(this.eventTags.getEndpointNameTag(), endpoint);
                        event.sequenceNumber = getInteger(this.eventTags.getEndpointHandlerSequenceNumberTag(), writingEndpointHandler);
                    }
                }
            }
        }
    }

    @GET
    @Path("/download/transaction/{application}/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public Response getDownloadTransaction(
            @QueryParam("q") String json,
            @PathParam("application") String applicationName,
            @PathParam("id") String transactionId) {
        EtmPrincipal etmPrincipal = getEtmPrincipal();
        Map<String, Object> valueMap = toMap(json);
        List<TransactionEvent> events = getTransactionEvents(applicationName, transactionId);
        if (events == null) {
            return null;
        }
        FileType fileType = FileType.valueOf(getString("fileType", valueMap).toUpperCase());
        File result = new QueryExporter().exportToFile(events, fileType, etmPrincipal);
        ResponseBuilder response = Response.ok(result);
        response.header("Content-Disposition", "attachment; filename=etm-" + applicationName.replaceAll(" ", "_") + "-" + transactionId + "." + fileType.name().toLowerCase());
        response.encoding(System.getProperty("file.encoding"));
        response.header("Content-Type", fileType.getContentType());
        return response.build();
    }


    @SuppressWarnings("unchecked")
    @GET
    @Path("/event/{type}/{id}/chain")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    @LegacyEndpointHandler("Remove old structure in earliestTransactionId determination")
    public String getEventChain(@PathParam("type") String eventType, @PathParam("id") String eventId) {
        IdsQueryBuilder idsQueryBuilder = new IdsQueryBuilder()
                .types(eventType)
                .addIds(eventId);
        // No principal filtered query. We would like to show the entire event chain, but the user should not be able to retrieve all information.
        SearchResponse response = enhanceRequest(client.prepareSearch(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL), etmConfiguration)
                .setQuery(idsQueryBuilder)
                .setFetchSource(new String[]{this.eventTags.getEndpointsTag() + ".*"}, null)
                .setFrom(0)
                .setSize(1)
                .get();
        if (response.getHits().getHits().length == 0) {
            return null;
        }
        SearchHit searchHit = response.getHits().getAt(0);
        Map<String, Object> source = searchHit.getSourceAsMap();
        List<Map<String, Object>> endpoints = (List<Map<String, Object>>) source.get(this.eventTags.getEndpointsTag());
        // Search for the earliest transaction id.
        long lowestTransactionHandling = Long.MAX_VALUE;
        String earliestTransactionId = null;
        if (endpoints != null) {
            for (Map<String, Object> endpoint : endpoints) {
                List<Map<String, Object>> endpointHandlers = getArray(this.eventTags.getEndpointHandlersTag(), endpoint);
                if (endpointHandlers != null) {
                    for (Map<String, Object> endpointHandler : endpointHandlers) {
                        if (endpointHandler.containsKey(this.eventTags.getEndpointHandlerTransactionIdTag())) {
                            String transactionId = (String) endpointHandler.get(this.eventTags.getEndpointHandlerTransactionIdTag());
                            long handlingTime = (long) endpointHandler.get(this.eventTags.getEndpointHandlerHandlingTimeTag());
                            if (handlingTime != 0 && handlingTime < lowestTransactionHandling) {
                                lowestTransactionHandling = handlingTime;
                                earliestTransactionId = transactionId;
                            }
                        }
                    }
                } else {
                    Map<String, Object> writingEndpointHandler = (Map<String, Object>) endpoint.get("writing_endpoint_handler");
                    if (writingEndpointHandler != null && writingEndpointHandler.containsKey(this.eventTags.getEndpointHandlerTransactionIdTag())) {
                        String transactionId = (String) writingEndpointHandler.get(this.eventTags.getEndpointHandlerTransactionIdTag());
                        long handlingTime = (long) writingEndpointHandler.get(this.eventTags.getEndpointHandlerHandlingTimeTag());
                        if (handlingTime != 0 && handlingTime < lowestTransactionHandling) {
                            lowestTransactionHandling = handlingTime;
                            earliestTransactionId = transactionId;
                        }
                    }
                    List<Map<String, Object>> readingEndpointHandlers = (List<Map<String, Object>>) endpoint.get("reading_endpoint_handlers");
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
        }
        if (earliestTransactionId == null) {
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
        for (EventChainApplication application : eventChain.getApplications()) {
            if (!first) {
                result.append(",");
            }
            result.append("{\"id\": ")
                    .append(escapeToJson(application.getId(), true))
                    .append(", \"label\": ")
                    .append(escapeToJson(application.getDisplayName(), true))
                    .append(", \"node_type\": \"application\"")
                    .append(", \"missing\": ")
                    .append(eventChain.isApplicationMissing(application))
                    .append("}");
            first = false;
        }
        for (EventChainEvent event : eventChain.getEvents().values()) {
            for (EventChainEndpoint endpoint : event.getEndpoints()) {
                // Add all endpoints as item.
                if (endpoint.getName() != null) {
                    if (!first) {
                        result.append(",");
                    }
                    result.append("{\"id\": ")
                            .append(escapeToJson(endpoint.getKey(), true))
                            .append(", \"label\": ")
                            .append(escapeToJson(endpoint.getName(), true))
                            .append(", \"event_id\": ")
                            .append(escapeToJson(event.getEventId(), true))
                            .append(", \"event_type\": ")
                            .append(escapeToJson(event.getEventType(), true))
                            .append(", \"node_type\": \"endpoint\"")
                            .append(", \"missing\": ")
                            .append(endpoint.isMissing())
                            .append("}");
                    first = false;
                }
                // Add the writer as item.
                if (endpoint.getWriter() != null) {
                    if (!first) {
                        result.append(",");
                    }
                    result.append("{\"id\": ")
                            .append(escapeToJson(endpoint.getWriter().getKey(), true))
                            .append(", \"label\": ")
                            .append(escapeToJson(endpoint.getWriter().getName(), true))
                            .append(", \"event_id\": ")
                            .append(escapeToJson(event.getEventId(), true))
                            .append(", \"event_type\": ")
                            .append(escapeToJson(event.getEventType(), true))
                            .append(", \"endpoint\": ")
                            .append(escapeToJson(endpoint.getName(), true))
                            .append(", \"transaction_id\": ")
                            .append(escapeToJson(endpoint.getWriter().getTransactionId(), true))
                            .append(", \"node_type\": \"event\"")
                            .append(", \"missing\": ")
                            .append(endpoint.getWriter().isMissing());
                    if (endpoint.getWriter().getApplication() != null) {
                        result.append(", \"parent\": ").append(escapeToJson(endpoint.getWriter().getApplication().getId(), true));
                    }
                    result.append("}");
                    first = false;
                }
                // Add all readers as item.
                for (EventChainItem item : endpoint.getReaders()) {
                    if (!first) {
                        result.append(",");
                    }
                    result.append("{\"id\": ").append(escapeToJson(item.getKey(), true)).append(", \"label\": ").append(escapeToJson(item.getName(), true)).append(", \"event_id\": ").append(escapeToJson(event.getEventId(), true)).append(", \"event_type\": ").append(escapeToJson(event.getEventType(), true)).append(", \"endpoint\": ").append(escapeToJson(endpoint.getName(), true)).append(", \"transaction_id\": ").append(escapeToJson(item.getTransactionId(), true)).append(", \"node_type\": \"event\"").append(", \"missing\": ").append(item.isMissing());
                    if (item.getApplication() != null) {
                        result.append(", \"parent\": ").append(escapeToJson(item.getApplication().getId(), true));
                    }
                    result.append("}");
                    first = false;
                }
            }
        }
        result.append("], \"edges\": [");
        first = true;
        for (EventChainEvent event : eventChain.getEvents().values()) {
            for (EventChainEndpoint endpoint : event.getEndpoints()) {
                if (endpoint.getName() != null) {
                    if (endpoint.getWriter() != null) {
                        if (!first) {
                            result.append(",");
                        }
                        // Add the connection from the writer to the endpoint.
                        result.append("{\"source\": ").append(escapeToJson(endpoint.getWriter().getKey(), true)).append(", \"target\": ").append(escapeToJson(endpoint.getKey(), true));
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
                        result.append("{ \"source\": ").append(escapeToJson(endpoint.getKey(), true)).append(", \"target\": ").append(escapeToJson(item.getKey(), true));
                        Float edgePercentage = eventChain.calculateEdgePercentageFromEndpointToItem(event, endpoint, item);
                        if (edgePercentage != null) {
                            result.append(", \"transition_time_percentage\": ").append(edgePercentage);
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
                            result.append("{ \"source\": ").append(escapeToJson(endpoint.getWriter().getKey(), true)).append(", \"target\": ").append(escapeToJson(item.getKey(), true));
                            Float edgePercentage = eventChain.calculateEdgePercentageFromEndpointToItem(event, endpoint, item);
                            if (edgePercentage != null) {
                                result.append(", \"transition_time_percentage\": ").append(edgePercentage);
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
                // writer then we lay a connection between the endpoints.
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
                        result.append("{ \"source\": ").append(escapeToJson(from, true)).append(", \"target\": ").append(escapeToJson(to, true));
                        Float edgePercentage = eventChain.calculateEdgePercentageFromItemToItem(lastRequestEndpoint.getWriter(), firstResponseEndpoint.getReaders().get(0));
                        if (edgePercentage != null) {
                            result.append(", \"transition_time_percentage\": ").append(edgePercentage);
                        }
                        result.append("}");
                        first = false;
                    }
                }
            }
            // TODO, achmea maatwerk om de dispatchers te koppelen aan de flows daarna.
        }
        for (EventChainTransaction transaction : eventChain.getTransactions().values()) {
            // Add connections between the events within a transaction
            int writerIx = 0;
            for (int i = 0; i < transaction.getReaders().size(); i++) {
                long endTime = Long.MAX_VALUE;
                if ((i + 1) < transaction.getReaders().size()) {
                    endTime = transaction.getReaders().get(i + 1).getHandlingTime();
                }
                EventChainItem reader = transaction.getReaders().get(i);
                for (; writerIx < transaction.getWriters().size() && transaction.getWriters().get(writerIx).getHandlingTime() < endTime; writerIx++) {
                    if (!first) {
                        result.append(",");
                    }
                    result.append("{ \"source\": ").append(escapeToJson(reader.getKey(), true)).append(", \"target\": ").append(escapeToJson(transaction.getWriters().get(writerIx).getKey(), true));
                    Float edgePercentage = eventChain.calculateEdgePercentageFromItemToItem(reader, transaction.getWriters().get(writerIx));
                    if (edgePercentage != null) {
                        result.append(", \"transition_time_percentage\": ").append(edgePercentage);
                    }
                    result.append("}");
                    first = false;
                }
            }

        }
        result.append("]}");
        return result.toString();
    }

    @LegacyEndpointHandler("findEventsQuery must contain a single must instead of 3 shoulds")
    private void addTransactionToEventChain(EventChain eventChain, String transactionId) {
        if (eventChain.containsTransaction(transactionId)) {
            return;
        }
        BoolQueryBuilder findEventsQuery = new BoolQueryBuilder()
                .minimumShouldMatch(1)
                .should(new TermQueryBuilder(this.eventTags.getEndpointsTag() +
                        "." + this.eventTags.getEndpointHandlersTag() +
                        "." + this.eventTags.getEndpointHandlerTransactionIdTag() + KEYWORD_SUFFIX, transactionId))
                .should(new TermQueryBuilder(this.eventTags.getEndpointsTag() +
                        ".reading_endpoint_handlers" +
                        "." + this.eventTags.getEndpointHandlerTransactionIdTag() + KEYWORD_SUFFIX, transactionId))
                .should(new TermQueryBuilder(this.eventTags.getEndpointsTag() +
                        ".writing_endpoint_handler" +
                        "." + this.eventTags.getEndpointHandlerTransactionIdTag() + KEYWORD_SUFFIX, transactionId));
        if (ElasticsearchLayout.OLD_EVENT_TYPES_PRESENT) {
            findEventsQuery.filter(QueryBuilders.boolQuery()
                    .should(QueryBuilders.termsQuery("_type", "http", "messaging"))
                    .should(QueryBuilders.boolQuery()
                            .must(QueryBuilders.termQuery("_type", ElasticsearchLayout.ETM_DEFAULT_TYPE))
                            .must(QueryBuilders.termsQuery(this.eventTags.getObjectTypeTag(), "http", "messaging")))
                    .minimumShouldMatch(1));
        } else {
            findEventsQuery.filter(QueryBuilders.termsQuery(this.eventTags.getObjectTypeTag(), "http", "messaging"));
        }
        // No principal filtered query. We would like to show the entire event chain, but the user should not be able to retrieve all information.
        SearchRequestBuilder searchRequest = enhanceRequest(client.prepareSearch(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL), etmConfiguration)
                .setQuery(findEventsQuery)
                .addSort(SortBuilders.fieldSort("_doc"))
                .setFetchSource(new String[]{
                        this.eventTags.getObjectTypeTag(),
                        this.eventTags.getEndpointsTag() + ".*",
                        this.eventTags.getExpiryTag(),
                        this.eventTags.getNameTag(),
                        this.eventTags.getCorrelationIdTag(),
                        this.eventTags.getMessagingEventTypeTag(),
                        this.eventTags.getHttpEventTypeTag()}, null
                );
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequest);
        if (!scrollableSearch.hasNext()) {
            return;
        }
        for (SearchHit searchHit : scrollableSearch) {
            Map<String, Object> source = searchHit.getSourceAsMap();
            String eventName = getString(this.eventTags.getNameTag(), source, "?");
            Long expiry = getLong(this.eventTags.getExpiryTag(), source);
            String subType = null;
            if ("http".equals(getEventType(searchHit))) {
                subType = getString(this.eventTags.getHttpEventTypeTag(), source);
            } else if ("messaging".equals(getEventType(searchHit))) {
                subType = getString(this.eventTags.getMessagingEventTypeTag(), source);
            }
            String correlationId = getString(this.eventTags.getCorrelationIdTag(), source);
            List<Map<String, Object>> endpoints = getArray(this.eventTags.getEndpointsTag(), source);
            if (endpoints != null) {
                for (Map<String, Object> endpoint : endpoints) {
                    String endpointName = getString(this.eventTags.getEndpointNameTag(), endpoint);
                    List<Map<String, Object>> endpointHandlers = getArray(this.eventTags.getEndpointHandlersTag(), endpoint);
                    if (endpointHandlers != null) {
                        for (Map<String, Object> eh : endpointHandlers) {
                            processEndpointHandlerForEventChain(eventChain,
                                    eh,
                                    EndpointHandler.EndpointHandlerType.WRITER.name().equals(getString(this.eventTags.getEndpointHandlerTypeTag(), eh)),
                                    searchHit.getId(),
                                    eventName,
                                    getEventType(searchHit),
                                    correlationId,
                                    subType,
                                    endpointName,
                                    transactionId,
                                    expiry);
                        }
                    } else {
                        addLegacyHandlersToEventChain(eventChain, searchHit, eventName, correlationId, subType, endpointName, transactionId, expiry, endpoint);
                    }
                }
            }
            // Check for request/response correlation and add those transactions as well.
            addRequestResponseConnectionToEventChain(eventChain, searchHit.getId(), correlationId, getEventType(searchHit), subType);
        }
    }

    @LegacyEndpointHandler("Method must be removed")
    private void addLegacyHandlersToEventChain(EventChain eventChain, SearchHit searchHit, String eventName, String correlationId, String subType, String endpointName, String transactionId, Long expiry, Map<String, Object> endpoint) {
        Map<String, Object> writingEndpointHandler = getObject("writing_endpoint_handler", endpoint);
        processEndpointHandlerForEventChain(eventChain,
                writingEndpointHandler,
                true,
                searchHit.getId(),
                eventName,
                getEventType(searchHit),
                correlationId,
                subType,
                endpointName,
                transactionId,
                expiry);
        List<Map<String, Object>> readingEndpointHandlers = getArray("reading_endpoint_handlers", endpoint);
        if (readingEndpointHandlers != null) {
            for (Map<String, Object> readingEndpointHandler : readingEndpointHandlers) {
                processEndpointHandlerForEventChain(eventChain,
                        readingEndpointHandler,
                        false,
                        searchHit.getId(),
                        eventName,
                        getEventType(searchHit),
                        correlationId,
                        subType,
                        endpointName,
                        transactionId,
                        expiry);
            }
        }

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
            String applicationInstance = null;
            if (endpointHandler.containsKey(this.eventTags.getEndpointHandlerApplicationTag())) {
                Map<String, Object> application = (Map<String, Object>) endpointHandler.get(this.eventTags.getEndpointHandlerApplicationTag());
                applicationName = getString(this.eventTags.getApplicationNameTag(), application);
                applicationInstance = getString(this.eventTags.getApplicationInstanceTag(), application);
            }
            Long responseTime = getLong(this.eventTags.getEndpointHandlerResponseTimeTag(), endpointHandler);
            if (transactionId.equals(handlerTransactionId)) {
                if (writer) {
                    eventChain.addWriter(eventId, transactionId, eventName, eventType, correlationId, subType, endpointName, applicationName, applicationInstance, handlingTime, responseTime, eventExpiry);
                } else {
                    eventChain.addReader(eventId, transactionId, eventName, eventType, correlationId, subType, endpointName, applicationName, applicationInstance, handlingTime, responseTime, eventExpiry);
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
                queryBuilder = new BoolQueryBuilder().must(new TermQueryBuilder(this.eventTags.getCorrelationIdTag() + KEYWORD_SUFFIX, id));
            } else if (MessagingEventType.RESPONSE.equals(messagingEventType) && correlationId != null) {
                queryBuilder = new IdsQueryBuilder().types("messaging").addIds(correlationId);
            }
        } else if ("http".equals(type)) {
            HttpEventType httpEventType = HttpEventType.safeValueOf(subType);
            if (HttpEventType.RESPONSE.equals(httpEventType) && correlationId != null) {
                queryBuilder = new IdsQueryBuilder().types("http").addIds(correlationId);
            } else {
                queryBuilder = new BoolQueryBuilder().must(new TermQueryBuilder(this.eventTags.getCorrelationIdTag() + KEYWORD_SUFFIX, id));
            }
        }
        if (queryBuilder == null) {
            return;
        }
        SearchResponse response = enhanceRequest(client.prepareSearch(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL), etmConfiguration)
                .setTypes(type)
                .setQuery(queryBuilder)
                .setFetchSource(new String[]{this.eventTags.getEndpointsTag() + ".*"}, null)
                .setFrom(0)
                .setSize(10)
                .get();
        if (response.getHits().getHits().length == 0) {
            return;
        }
        for (SearchHit searchHit : response.getHits().getHits()) {
            Map<String, Object> source = searchHit.getSourceAsMap();
            List<Map<String, Object>> endpoints = getArray(this.eventTags.getEndpointsTag(), source);
            if (endpoints == null) {
                continue;
            }
            for (Map<String, Object> endpoint : endpoints) {
                List<Map<String, Object>> endpointHandlers = getArray(this.eventTags.getEndpointHandlersTag(), endpoint);
                if (endpointHandlers != null) {
                    for (Map<String, Object> eh : endpointHandlers) {
                        String transactionId = getString(this.eventTags.getEndpointHandlerTransactionIdTag(), eh);
                        if (transactionId != null) {
                            addTransactionToEventChain(eventChain, transactionId);
                        }
                    }
                } else {
                    addLegacyRequestResponseToEventChain(eventChain, endpoint);
                }
            }
        }

    }

    @LegacyEndpointHandler("Remove this method")
    private void addLegacyRequestResponseToEventChain(EventChain eventChain, Map<String, Object> endpoint) {
        Map<String, Object> writingEndpointHandler = getObject("writing_endpoint_handler", endpoint);
        if (writingEndpointHandler != null) {
            String transactionId = getString(this.eventTags.getEndpointHandlerTransactionIdTag(), writingEndpointHandler);
            if (transactionId != null) {
                addTransactionToEventChain(eventChain, transactionId);
            }
        }
        List<Map<String, Object>> readingEndpointHandlers = getArray("reading_endpoint_handlers", endpoint);
        if (readingEndpointHandlers != null) {
            for (Map<String, Object> readingEndpointHandler : readingEndpointHandlers) {
                String transactionId = getString(this.eventTags.getEndpointHandlerTransactionIdTag(), readingEndpointHandler);
                if (transactionId != null) {
                    addTransactionToEventChain(eventChain, transactionId);
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
            addStringElementToJsonBuffer("index", searchHit.getIndex(), result, true);
            addStringElementToJsonBuffer("type", searchHit.getType(), result, false);
            addStringElementToJsonBuffer("id", searchHit.getId(), result, false);
            result.append(", \"source\": ").append(searchHit.getSourceAsString());
            result.append("}");
        }
    }


    /**
     * Gives the event type of a <code>SearchHit</code> instance.
     *
     * @param searchHit The <code>SearchHit</code> to determine the event type for.
     * @return The event type, or <code>null</code> if the event type cannot determined.
     */
    private String getEventType(SearchHit searchHit) {
        if (searchHit == null) {
            return null;
        }
        if (ElasticsearchLayout.ETM_DEFAULT_TYPE.equals(searchHit.getType())) {
            return getString(this.eventTags.getObjectTypeTag(), searchHit.getSourceAsMap());
        } else {
            return searchHit.getType();
        }
    }
}
