package com.jecstar.etm.gui.rest.services.search;

import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.gui.rest.export.FileType;
import com.jecstar.etm.gui.rest.export.QueryExporter;
import com.jecstar.etm.gui.rest.services.AbstractIndexMetadataService;
import com.jecstar.etm.gui.rest.services.Keyword;
import com.jecstar.etm.gui.rest.services.search.graphs.*;
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
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.SyncActionListener;
import com.jecstar.etm.server.core.elasticsearch.builder.GetRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.IndexRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.UpdateRequestBuilder;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.EtmQueryBuilder;
import com.jecstar.etm.server.core.persisting.RequestEnhancer;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import com.jecstar.etm.server.core.util.DateUtils;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
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

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Path("/search")
@DeclareRoles(SecurityRoles.ALL_ROLES)
public class SearchService extends AbstractIndexMetadataService {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(SearchService.class);

    private static final DateTimeFormatter dateTimeFormatterIndexPerDay = DateUtils.getIndexPerDayFormatter();

    private static DataRepository dataRepository;
    private static EtmConfiguration etmConfiguration;
    private static RequestEnhancer requestEnhancer;

    private final TelemetryEventTags eventTags = new TelemetryEventTagsJsonImpl();
    private final EtmConfigurationTags configurationTags = new EtmConfigurationTagsJsonImpl();
    private final QueryAuditLogConverter queryAuditLogConverter = new QueryAuditLogConverter();
    private final GetEventAuditLogConverter getEventAuditLogConverter = new GetEventAuditLogConverter();

    public static void initialize(DataRepository dataRepository, EtmConfiguration etmConfiguration) {
        SearchService.dataRepository = dataRepository;
        SearchService.etmConfiguration = etmConfiguration;
        SearchService.requestEnhancer = new RequestEnhancer(etmConfiguration);
    }

    @GET
    @Path("/userdata")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String getSearchTemplates() {
        EtmPrincipal etmPrincipal = getEtmPrincipal();
        GetResponse getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + etmPrincipal.getId())
                .setFetchSource(new String[]{
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + ".search_templates",
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.configurationTags.getSearchHistoryTag(),
                }, null));
        Map<String, Object> valueMap;
        if (getResponse.isSourceEmpty() || getResponse.getSourceAsMap().isEmpty() || !getResponse.getSourceAsMap().containsKey(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER)) {
            valueMap = new HashMap<>();
        } else {
            valueMap = getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, getResponse.getSourceAsMap(), Collections.emptyMap());

        }
        valueMap.put("max_search_templates", etmConfiguration.getMaxSearchTemplateCount());
        valueMap.put("default_search_range", etmPrincipal.getDefaultSearchRange());
        valueMap.put("timeZone", etmPrincipal.getTimeZone().getID());
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
        UpdateRequestBuilder builder = requestEnhancer.enhance(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + getEtmPrincipal().getId())
        )
                .setScript(new Script(ScriptType.STORED, null, "etm_update-search-template", scriptParams));
        dataRepository.update(builder);
        return "{ \"status\": \"success\" }";
    }

    @DELETE
    @Path("/templates/{templateName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String removeSearchTemplate(@PathParam("templateName") String templateName) {
        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put("name", templateName);
        UpdateRequestBuilder builder = requestEnhancer.enhance(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + getEtmPrincipal().getId())
        )
                .setScript(new Script(ScriptType.STORED, null, "etm_remove-search-template", scriptParams));
        dataRepository.update(builder);
        return "{ \"status\": \"success\" }";
    }

    @GET
    @Path("/keywords/{indexName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String getKeywords(@PathParam("indexName") String indexName) {
        StringBuilder result = new StringBuilder();
        List<Keyword> keywords = getIndexFields(SearchService.dataRepository, indexName);
        result.append("{ \"keywords\":[");
        result.append("{");
        result.append("\"index\": ").append(escapeToJson(indexName, true)).append(",");
        result.append("\"keywords\": [").append(keywords.stream().map(n ->
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
        boolean payloadVisible = etmPrincipal.isInAnyRole(SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WRITE);

        Instant now = Instant.now();
        QueryAuditLogBuilder auditLogBuilder = new QueryAuditLogBuilder().setTimestamp(now).setHandlingTime(now).setPrincipalId(etmPrincipal.getId());

        SearchRequestParameters parameters = new SearchRequestParameters(toMap(json), etmPrincipal);
        SearchRequestBuilder requestBuilder = createRequestFromInput(parameters, etmPrincipal);
        NumberFormat numberFormat = NumberFormat.getInstance(etmPrincipal.getLocale());
        SearchResponse response = dataRepository.search(requestBuilder);
        StringBuilder result = new StringBuilder();
        result.append("{");
        result.append("\"status\": \"success\"");
        result.append(",\"history_size\": ").append(etmPrincipal.getHistorySize());
        result.append(",\"hits\": ").append(response.getHits().getTotalHits().value);
        result.append(",\"hits_relation\": \"").append(response.getHits().getTotalHits().relation.name()).append("\"");
        result.append(",\"hits_as_string\": \"").append(numberFormat.format(response.getHits().getTotalHits().value)).append((TotalHits.Relation.GREATER_THAN_OR_EQUAL_TO.equals(response.getHits().getTotalHits().relation)) ? "+\"" : "\"");
        result.append(",\"time_zone\": \"").append(etmPrincipal.getTimeZone().getID()).append("\"");
        result.append(",\"start_ix\": ").append(parameters.getStartIndex());
        result.append(",\"end_ix\": ").append(parameters.getStartIndex() + response.getHits().getHits().length - 1);
        // TODO has_more_results is inaccurate in etm 4 because totalhits is a GTE value.
        result.append(",\"has_more_results\": ").append(parameters.getStartIndex() + response.getHits().getHits().length < response.getHits().getTotalHits().value - 1);
        result.append(",\"time_zone\": \"").append(etmPrincipal.getTimeZone().getID()).append("\"");
        result.append(",\"max_downloads\": ").append(etmConfiguration.getMaxSearchResultDownloadRows());
        result.append(",\"may_see_payload\": ").append(payloadVisible);
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
                requestBuilder.build().source().query().toXContent(contentBuilder, ToXContent.EMPTY_PARAMS);
                executedQuery = Strings.toString(contentBuilder);
            } catch (IOException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage(e.getMessage(), e);
                }
            }
            auditLogBuilder
                    .setUserQuery(parameters.getQueryString())
                    .setExectuedQuery(executedQuery)
                    .setNumberOfResults(response.getHits().getTotalHits().value)
                    .setNumberOfResultsRelation(response.getHits().getTotalHits().relation.name())
                    .setQueryTime(queryTime);
            IndexRequestBuilder builder = requestEnhancer.enhance(
                    new IndexRequestBuilder(ElasticsearchLayout.AUDIT_LOG_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(now))
            )
                    .setSource(this.queryAuditLogConverter.write(auditLogBuilder.build()), XContentType.JSON);
            dataRepository.indexAsync(builder, DataRepository.noopActionListener());
        }
        return result.toString();
    }

    private SearchRequestBuilder createRequestFromInput(SearchRequestParameters parameters, EtmPrincipal etmPrincipal) {
        if (!parameters.hasFields()) {
            parameters.addField(this.eventTags.getEndpointsTag() + "." + this.eventTags.getEndpointHandlersTag() + "." + this.eventTags.getEndpointHandlerHandlingTimeTag());
            parameters.addField(this.eventTags.getNameTag());
        }
        EtmQueryBuilder etmQueryBuilder = new EtmQueryBuilder(parameters.getQueryString(), parameters.getTypes(), dataRepository, SearchService.requestEnhancer)
                .setTimeZone(etmPrincipal.getTimeZone().getID());

        var notAfterFilterNecessary = true;
        var needsUtcZone = false;
        if (parameters.getEndTime() != null || parameters.getStartTime() != null) {
            var timestampFilter = new RangeQueryBuilder(parameters.getTimeFilterField());
            if (parameters.getEndTime() != null) {
                try {
                    // Check if the endtime is given as an exact timestamp or an elasticsearch date math.
                    var endTime = Long.parseLong(parameters.getEndTime());
                    timestampFilter.lte(endTime < parameters.getNotAfterTimestamp() ? endTime : parameters.getNotAfterTimestamp());
                    notAfterFilterNecessary = false;
                    needsUtcZone = true;
                } catch (NumberFormatException e) {
                    // Endtime was an elasticsearch date math. Check if it is the exact value of "now"
                    if ("now" .equalsIgnoreCase(parameters.getEndTime())) {
                        // Replace "now" with the notAfter timestamp which is in essence the same
                        timestampFilter.lte(parameters.getNotAfterTimestamp());
                        notAfterFilterNecessary = false;
                        needsUtcZone = true;
                    } else {
                        timestampFilter.lte(parameters.getEndTime());
                    }
                }
            } else {
                timestampFilter.lte(parameters.getNotAfterTimestamp());
                needsUtcZone = true;
            }
            if (parameters.getStartTime() != null) {
                try {
                    // Check if the start time is given as an exact timestamp or an elasticsearch date math.
                    var endTime = Long.parseLong(parameters.getStartTime());
                    timestampFilter.gte(endTime);
                    needsUtcZone = true;
                } catch (NumberFormatException e) {
                    timestampFilter.gte(parameters.getStartTime());
                }
            }
            if (!needsUtcZone) {
                timestampFilter.timeZone(etmPrincipal.getTimeZone().getID());
            }
            etmQueryBuilder.filterRoot(timestampFilter).filterJoin(timestampFilter);
        }
        if (notAfterFilterNecessary) {
            // the given parameters.getStartTime() & parameters.getEndTime() were zero or an elasticsearch math date. We have to apply the notAfterTime filter as well.
            var rangeQueryFilter = new RangeQueryBuilder("timestamp").lte(parameters.getNotAfterTimestamp());
            etmQueryBuilder.filterRoot(rangeQueryFilter).filterJoin(rangeQueryFilter);
        }
        if (parameters.getTypes().size() != 5) {
            etmQueryBuilder.filterRoot(QueryBuilders.termsQuery(this.eventTags.getObjectTypeTag(), parameters.getTypes().toArray()));
        }
        var requestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL))
                .setQuery(addFilterQuery(getEtmPrincipal(), etmQueryBuilder.buildRootQuery()))
                .setFetchSource(parameters.getFields().toArray(new String[0]), null)
                .setFrom(parameters.getStartIndex())
                .setSize(parameters.getMaxResults() > 500 ? 500 : parameters.getMaxResults());
        if (parameters.getSortField() != null && parameters.getSortField().trim().length() > 0) {
            String sortProperty = getSortProperty(dataRepository, ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL, parameters.getSortField());
            if (sortProperty != null) {
                requestBuilder.setSort(sortProperty, "desc".equals(parameters.getSortOrder()) ? SortOrder.DESC : SortOrder.ASC);
            }
        }
        return requestBuilder;
    }

    private void writeQueryHistory(long timestamp, SearchRequestParameters parameters, EtmPrincipal etmPrincipal, int history_size) {
        var scriptParams = new HashMap<String, Object>();
        var query = new HashMap<String, Object>();
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
        var builder = requestEnhancer.enhance(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + etmPrincipal.getId())
        ).setScript(new Script(ScriptType.STORED, null, "etm_update-search-history", scriptParams));
        dataRepository.updateAsync(builder, DataRepository.noopActionListener());
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

        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, requestBuilder, parameters.getStartIndex());
        FileType fileType = FileType.valueOf(getString("fileType", valueMap).toUpperCase());
        File result = new QueryExporter().exportToFile(
                scrollableSearch,
                fileType,
                Math.min(parameters.getMaxResults(),
                        etmConfiguration.getMaxSearchResultDownloadRows()),
                etmPrincipal,
                parameters.toFieldLayouts(),
                c -> dataRepository.indexAsync(
                        requestEnhancer.enhance(
                                new IndexRequestBuilder(ElasticsearchLayout.AUDIT_LOG_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(c.getTimestamp()))
                        )
                                .setSource(this.getEventAuditLogConverter.write(c.build()), XContentType.JSON),
                        DataRepository.noopActionListener()
                )
        );
        ResponseBuilder response = Response.ok(result);
        response.header("Content-Disposition", "attachment; filename=etm-results." + fileType.name().toLowerCase());
        response.encoding(System.getProperty("file.encoding"));
        response.header("Content-Type", fileType.getContentType());
        return response.build();
    }

    @GET
    @Path("/event/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String getEvent(@PathParam("id") String eventId) {
        Instant now = Instant.now();
        boolean payloadVisible = getEtmPrincipal().maySeeEventPayload();
        GetEventAuditLogBuilder auditLogBuilder = new GetEventAuditLogBuilder()
                .setTimestamp(now)
                .setHandlingTime(now)
                .setPrincipalId(getEtmPrincipal().getId())
                .setEventId(eventId)
                .setPayloadVisible(payloadVisible)
                .setDownloaded(false)
                .setFound(false);
        SyncActionListener<SearchResponse> auditLogListener = null;
        if (getEtmPrincipal().isInRole(SecurityRoles.AUDIT_LOG_READ)) {
            auditLogListener = DataRepository.syncActionListener(etmConfiguration.getQueryTimeout());
            // We've got an principal with audit logs roles requesting the event. Also add the audit logs of this event to the response.
            findAuditLogsForEvent(eventId, auditLogListener);
        }
        StringBuilder result = new StringBuilder();
        result.append("{");
        addStringElementToJsonBuffer("time_zone", getEtmPrincipal().getTimeZone().getID(), result, true);
        SearchHit searchHit = getEvent(eventId, new String[]{"*"}, !payloadVisible ? new String[]{this.eventTags.getPayloadTag()} : null);
        if (searchHit != null) {
            auditLogBuilder.setFound(true);
            Map<String, Object> valueMap = searchHit.getSourceAsMap();
            result.append(", \"event\": {");
            addStringElementToJsonBuffer("index", searchHit.getIndex(), result, true);
            addStringElementToJsonBuffer("id", searchHit.getId(), result, false);
            result.append(", \"source\": ").append(toString(valueMap));
            result.append("}");
            // Add the name to the audit log.
            auditLogBuilder.setEventName(getString(this.eventTags.getNameTag(), valueMap));
            // Try to find an event this event is correlating to.
            String correlatedToId = getString(this.eventTags.getCorrelationIdTag(), valueMap);
            boolean correlationAdded = false;
            if (correlatedToId != null && !correlatedToId.equals(eventId)) {
                SearchHit correlatedEvent = conditionallyGetEvent(correlatedToId, payloadVisible);
                if (correlatedEvent != null) {
                    result.append(", \"correlated_events\": [");
                    result.append("{");
                    addStringElementToJsonBuffer("index", correlatedEvent.getIndex(), result, true);
                    addStringElementToJsonBuffer("id", correlatedEvent.getId(), result, false);
                    result.append(", \"source\": ").append(correlatedEvent.getSourceAsString());
                    result.append("}");
                    correlationAdded = true;
                    auditLogBuilder.addCorrelatedEvent(correlatedEvent.getId());
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
                    SearchHit correlatedEvent = conditionallyGetEvent(correlationId, payloadVisible);
                    if (correlatedEvent != null) {
                        added++;
                        if (!correlationAdded) {
                            result.append(", \"correlated_events\": [");
                        } else {
                            result.append(",");
                        }
                        result.append("{");
                        addStringElementToJsonBuffer("index", correlatedEvent.getIndex(), result, true);
                        addStringElementToJsonBuffer("id", correlatedEvent.getId(), result, false);
                        result.append(", \"source\": ").append(correlatedEvent.getSourceAsString());
                        result.append("}");
                        correlationAdded = true;
                        auditLogBuilder.addCorrelatedEvent(correlatedEvent.getId());
                    }
                }
            }
            if (correlationAdded) {
                result.append("]");
            }
            if (auditLogListener != null) {
                // Audit logs received async. Wait here to add the to the result.
                SearchResponse auditLogResponse = auditLogListener.get();
                if (auditLogResponse != null && auditLogResponse.getHits().getHits().length != 0) {
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
                        addBooleanElementToJsonBuffer("payload_visible", getBoolean(GetEventAuditLog.PAYLOAD_VISIBLE, auditLogValues, true), result, false);
                        addBooleanElementToJsonBuffer("downloaded", getBoolean(GetEventAuditLog.DOWNLOADED, auditLogValues, false), result, false);
                        result.append("}");
                    }
                    result.append("]");
                }
            }

        }
        result.append("}");
        // Log the retrieval request to the audit logs.
        IndexRequestBuilder builder = requestEnhancer.enhance(
                new IndexRequestBuilder(ElasticsearchLayout.AUDIT_LOG_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(now))
        )
                .setSource(this.getEventAuditLogConverter.write(auditLogBuilder.build()), XContentType.JSON);
        dataRepository.indexAsync(builder, DataRepository.noopActionListener());
        return result.toString();
    }

    private void findAuditLogsForEvent(String eventId, ActionListener<SearchResponse> actionListener) {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(new TermQueryBuilder(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.AUDIT_LOG_OBJECT_TYPE_GET_EVENT));
        boolQueryBuilder.should(new TermQueryBuilder(GetEventAuditLog.EVENT_ID + KEYWORD_SUFFIX, eventId));
        boolQueryBuilder.should(new TermQueryBuilder(GetEventAuditLog.CORRELATED_EVENTS + "." + GetEventAuditLog.EVENT_ID + KEYWORD_SUFFIX, eventId));
        boolQueryBuilder.minimumShouldMatch(1);
        SearchRequestBuilder requestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL))
                .setQuery(boolQueryBuilder)
                .setSort(AuditLog.HANDLING_TIME, SortOrder.DESC)
                .setFetchSource(new String[]{AuditLog.HANDLING_TIME, AuditLog.PRINCIPAL_ID, GetEventAuditLog.EVENT_ID, GetEventAuditLog.PAYLOAD_VISIBLE, GetEventAuditLog.DOWNLOADED}, null)
                .setFrom(0)
                .setSize(500);
        dataRepository.searchAsync(requestBuilder, actionListener);
    }

    @GET
    @Path("/event/{id}/dag")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String getDirectedGraphData(@PathParam("id") String eventId) {
        var directedGraph = calculateDirectedGraph(eventId);
        if (directedGraph == null) {
            return null;
        }
        var layers = directedGraph.getLayers();
        var result = new StringBuilder();
        result.append("{");
        result.append("\"locale\": ").append(getLocalFormatting(getEtmPrincipal()));
        var firstLayer = true;
        result.append(", \"layers\": [");
        for (var layer : layers) {
            if (firstLayer) {
                result.append("{");
                firstLayer = false;
            } else {
                result.append(", {");
            }
            var firstVertex = true;
            result.append("\"vertices\": [");
            for (var vertex : layer.getVertices()) {
                if (firstVertex) {
                    result.append("{");
                    firstVertex = false;
                } else {
                    result.append(", {");
                }
                vertex.toJson(result, true);
                var childVertices = layer.getChildVertices(vertex);
                if (childVertices != null && childVertices.size() > 0) {
                    var firstChild = true;
                    result.append(", \"children\": [");
                    for (var childVertex : childVertices) {
                        if (firstChild) {
                            result.append("{");
                            firstChild = false;
                        } else {
                            result.append(", {");
                        }
                        childVertex.toJson(result, true);
                        result.append("}");
                    }
                    result.append("]");
                }
                result.append("}");
            }
            result.append("]}");
        }
        result.append("]");
        var firstEdge = true;
        result.append(", \"edges\": [");
        Duration totalEventTime = directedGraph.getTotalEventTime();
        if (totalEventTime.toMillis() <= 0) {
            totalEventTime = null;
        }
        for (var vertex : directedGraph.getVertices()) {
            for (var adjVertex : directedGraph.getAdjacentOutVertices(vertex)) {
                if (firstEdge) {
                    result.append("{");
                    firstEdge = false;
                } else {
                    result.append(", {");
                }
                addStringElementToJsonBuffer("from", vertex.getVertexId(), result, true);
                addStringElementToJsonBuffer("to", adjVertex.getVertexId(), result, false);
                if (totalEventTime != null) {
                    Instant startTime = null;
                    Instant endTime = null;
                    if (vertex instanceof Event && adjVertex instanceof Event) {
                        startTime = ((Event) vertex).getEventStartTime();
                        endTime = ((Event) adjVertex).getEventStartTime();
                    } else if (vertex instanceof Endpoint && adjVertex instanceof Event) {
                        startTime = ((Endpoint) vertex).getWriteTime();
                        endTime = ((Event) adjVertex).getEventStartTime();
                    } else if (vertex instanceof Endpoint && adjVertex instanceof Endpoint) {
                        startTime = ((Endpoint) vertex).getWriteTime();
                        endTime = ((Endpoint) adjVertex).getFirstReadTime();
                    }
                    if (startTime != null && endTime != null) {
                        long transitionTime = endTime.toEpochMilli() - startTime.toEpochMilli();
                        addLongElementToJsonBuffer("transition_time", transitionTime, result, false);
                        addDoubleElementToJsonBuffer("transition_time_percentage", (double) ((float) transitionTime / (float) totalEventTime.toMillis()), result, false);
                    }
                }
                result.append("}");
            }
        }
        result.append("]}");
        return result.toString();

    }

    @GET
    @Path("/event/{id}/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String getEventChainEndpoint(@PathParam("id") String eventId) {
        StringBuilder result = new StringBuilder();
        result.append("{");
        addStringElementToJsonBuffer("time_zone", getEtmPrincipal().getTimeZone().getID(), result, true);
        SearchHit searchHit = getEvent(eventId, new String[]{this.eventTags.getEndpointsTag() + ".*"}, null);
        if (searchHit != null) {
            result.append(", \"event\": {");
            addStringElementToJsonBuffer("index", searchHit.getIndex(), result, true);
            addStringElementToJsonBuffer("id", searchHit.getId(), result, false);
            result.append(", \"source\": ").append(searchHit.getSourceAsString());
            result.append("}");
        }
        result.append("}");
        return result.toString();
    }

    private SearchHit getEvent(String eventId, String[] includes, String[] excludes) {
        IdsQueryBuilder idsQueryBuilder = new IdsQueryBuilder()
                .addIds(eventId);
        BoolQueryBuilder query = new BoolQueryBuilder();
        query.must(idsQueryBuilder);
        SearchRequestBuilder builder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL))
                .setQuery(addFilterQuery(getEtmPrincipal(), query))
                .setFrom(0)
                .setSize(1);
        builder.setFetchSource(includes, excludes);
        SearchResponse response = dataRepository.search(builder);
        if (response.getHits().getHits().length == 0) {
            return null;
        }
        return response.getHits().getAt(0);
    }

    private SearchHit conditionallyGetEvent(String eventId, boolean payloadVisible) {
        IdsQueryBuilder idsQueryBuilder = new IdsQueryBuilder()
                .addIds(eventId);
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder().must(idsQueryBuilder);
        SearchRequestBuilder builder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL))
                .setQuery(alwaysShowCorrelatedEvents(getEtmPrincipal()) ? idsQueryBuilder : addFilterQuery(getEtmPrincipal(), boolQueryBuilder))
                .setFrom(0)
                .setSize(1)
                .setFetchSource(new String[]{"*"}, !payloadVisible ? new String[]{this.eventTags.getPayloadTag()} : null);
        SearchResponse response = dataRepository.search(builder);
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
    @Path("/transaction/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String getTransaction(@PathParam("id") String transactionId) {
        List<TransactionEvent> events = getTransactionEvents(transactionId);
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

    private List<TransactionEvent> getTransactionEvents(String transactionId) {
        BoolQueryBuilder findEventsQuery = new BoolQueryBuilder()
                .must(new TermQueryBuilder(this.eventTags.getEndpointsTag() +
                        "." + this.eventTags.getEndpointHandlersTag() +
                        "." + this.eventTags.getEndpointHandlerTransactionIdTag() + KEYWORD_SUFFIX, transactionId)
                );
        SearchRequestBuilder searchRequest = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL))
                .setQuery(addFilterQuery(getEtmPrincipal(), findEventsQuery))
                .setSort(SortBuilders.fieldSort("_doc"))
                .setFetchSource(new String[]{
                        this.eventTags.getObjectTypeTag(),
                        this.eventTags.getEndpointsTag() + ".*",
                        this.eventTags.getNameTag(),
                        this.eventTags.getPayloadTag(),
                        this.eventTags.getMessagingEventTypeTag(),
                        this.eventTags.getHttpEventTypeTag(),
                        this.eventTags.getSqlEventTypeTag()}, null
                );
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequest);
        if (!scrollableSearch.hasNext()) {
            scrollableSearch.clearScrollIds();
            return null;
        }
        List<TransactionEvent> events = new ArrayList<>();
        for (SearchHit searchHit : scrollableSearch) {
            TransactionEvent event = new TransactionEvent();
            Map<String, Object> source = searchHit.getSourceAsMap();
            event.index = searchHit.getIndex();
            event.objectType = getString(this.eventTags.getObjectTypeTag(), source);
            event.id = searchHit.getId();
            event.name = getString(this.eventTags.getNameTag(), source);
            event.payload = getString(this.eventTags.getPayloadTag(), source);
            List<Map<String, Object>> endpoints = getArray(this.eventTags.getEndpointsTag(), source);
            if (endpoints != null) {
                for (Map<String, Object> endpoint : endpoints) {
                    List<Map<String, Object>> endpointHandlers = getArray(this.eventTags.getEndpointHandlersTag(), endpoint);
                    if (endpointHandlers != null) {
                        for (Map<String, Object> eh : endpointHandlers) {
                            if (isWithinTransaction(eh, transactionId)) {
                                event.handlingTime = getLong(this.eventTags.getEndpointHandlerHandlingTimeTag(), eh);
                                event.direction = EndpointHandler.EndpointHandlerType.WRITER.name().equals(getString(this.eventTags.getEndpointHandlerTypeTag(), eh)) ? "outgoing" : "incoming";
                                event.endpoint = getString(this.eventTags.getEndpointNameTag(), endpoint);
                                event.sequenceNumber = getInteger(this.eventTags.getEndpointHandlerSequenceNumberTag(), eh);
                            }
                        }
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

    @GET
    @Path("/download/transaction/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public Response getDownloadTransaction(
            @QueryParam("q") String json,
            @PathParam("id") String transactionId) {
        EtmPrincipal etmPrincipal = getEtmPrincipal();
        Map<String, Object> valueMap = toMap(json);
        List<TransactionEvent> events = getTransactionEvents(transactionId);
        if (events == null) {
            return null;
        }
        FileType fileType = FileType.valueOf(getString("fileType", valueMap).toUpperCase());
        File result = new QueryExporter().exportToFile(
                events,
                fileType,
                etmPrincipal,
                c -> dataRepository.indexAsync(requestEnhancer.enhance(
                        new IndexRequestBuilder(ElasticsearchLayout.AUDIT_LOG_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(c.getTimestamp()))
                )
                        .setSource(this.getEventAuditLogConverter.write(c.build()), XContentType.JSON), DataRepository.noopActionListener())

        );
        ResponseBuilder response = Response.ok(result);
        response.header("Content-Disposition", "attachment; filename=etm-" + transactionId + "." + fileType.name().toLowerCase());
        response.encoding(System.getProperty("file.encoding"));
        response.header("Content-Type", fileType.getContentType());
        return response.build();
    }

    @GET
    @Path("/event/{id}/chain")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String getEventChain(@PathParam("id") String eventId) {
        var directedGraph = calculateDirectedGraph(eventId);
        if (directedGraph == null) {
            return null;
        }
        List<Event> events = directedGraph.findEvents(x -> !x.isResponse());
        events.sort(Comparator.comparing(Event::getEventStartTime).thenComparing(Event::getOrder).thenComparing(Event::getEventEndTime, Comparator.reverseOrder()));

        EtmPrincipal etmPrincipal = getEtmPrincipal();
        return "{" +
                "\"locale\": " + getLocalFormatting(etmPrincipal) +
                ", \"chart_config\": {" +
                "\"credits\": {\"enabled\": false}" +
                ", \"legend\": {\"enabled\": false}" +
                ", \"time\": {\"timezone\": " + escapeToJson(etmPrincipal.getTimeZone().toZoneId().toString(), true) + "}" +
                ", \"chart\": {\"type\": \"xrange\"}" +
                ", \"title\": {\"text\": \"Event chain times\"}" +
                ", \"xAxis\": {\"type\": \"datetime\"}" +
                ", \"yAxis\": {\"title\": { \"text\": \"Events\"}, \"reversed\": true, \"categories\": [" +
                events.stream().map(e -> escapeToJson(e.getName() + (e.isSent() ? " (sent)" : " (received)"), true)).collect(Collectors.joining(",")) +
                "]}" +
                ", \"series\": [{\"name\": \"Chain overview\", \"pointPadding\": 0, \"colorByPoint\": false, \"colorIndex\": 7, \"data\": [" +
                IntStream.range(0, events.size())
                        .mapToObj(i -> "{ \"x\": "
                                + events.get(i).getEventStartTime().toEpochMilli()
                                + ", \"x2\": " + (events.get(i).getEventEndTime() != null ? events.get(i).getEventEndTime().toEpochMilli() : events.get(i).getEventStartTime().toEpochMilli() + 10)
                                + ",\"y\": " + i
                                + ",\"partialFill\": " + (events.get(i).getAbsoluteTransactionPercentage() != null ? events.get(i).getAbsoluteTransactionPercentage().toString() : 0.0)
                                + ",\"dataLabels\": {\"enabled\": " + (events.get(i).getAbsoluteTransactionPercentage() != null ? "true" : "false") + "}"
                                + ",\"event_time\": " + escapeToJson(events.get(i).getTotalEventTime() != null ? etmPrincipal.getNumberFormat().format(events.get(i).getTotalEventTime().toMillis()) : null, true)
                                + ",\"event_absolute_time\": " + escapeToJson(events.get(i).getAbsoluteDuration() != null ? etmPrincipal.getNumberFormat().format(events.get(i).getAbsoluteDuration().toMillis()) : null, true)
                                + ",\"event_id\": " + escapeToJson(events.get(i).getEventId(), true)
                                + ",\"endpoint\": " + escapeToJson(events.get(i).getEndpointName(), true)
                                + ",\"application\": " + (events.get(i).getParent() != null ? escapeToJson(events.get(i).getParent().getName(), true) : null)
                                + ",\"application_instance\": " + (events.get(i).getParent() != null ? escapeToJson(events.get(i).getParent().getInstance(), true) : null)
                                + ",\"transaction_id\": " + escapeToJson(events.get(i).getTransactionId(), true) + "}")
                        .collect(Collectors.joining(",")) +
                "], \"tooltip\": { \"pointFormat\": " + escapeToJson("Name: <b>{point.yCategory}</b><br/>Application: <b>{point.application}</b><br/>Application instance: <b>{point.application_instance}</b><br/>Endpoint: <b>{point.endpoint}</b><br/>Response time: <b>{point.event_time}ms</b><br/>Absolute time: <b>{point.event_absolute_time}ms</b><br/>", true) + "}}]" +
                "}}";
    }

    /**
     * Creates a <code>DirectedGraph</code> for a given event.
     *
     * @param eventId The event id.
     * @return The <code>DirectedGraph</code> for the event.
     */
    @SuppressWarnings("unchecked")
    private DirectedGraph calculateDirectedGraph(String eventId) {
        IdsQueryBuilder idsQueryBuilder = new IdsQueryBuilder()
                .addIds(eventId);
        // No principal filtered query. We would like to show the entire event chain, but the user should not be able to retrieve all information.
        SearchRequestBuilder builder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL))
                .setQuery(idsQueryBuilder)
                .setFetchSource(null, this.eventTags.getPayloadTag())
                .setFrom(0)
                .setSize(1);
        SearchResponse response = dataRepository.search(builder);
        if (response.getHits().getHits().length == 0) {
            return null;
        }
        var directedGraph = new DirectedGraph();
        var handledTransactions = new HashSet<String>();
        var searchHit = response.getHits().getAt(0);
        var source = searchHit.getSourceAsMap();
        var endpoints = (List<Map<String, Object>>) source.get(this.eventTags.getEndpointsTag());
        if (endpoints != null) {
            for (var endpoint : endpoints) {
                List<Map<String, Object>> endpointHandlers = getArray(this.eventTags.getEndpointHandlersTag(), endpoint);
                if (endpointHandlers != null) {
                    for (var endpointHandler : endpointHandlers) {
                        if (endpointHandler.containsKey(this.eventTags.getEndpointHandlerTransactionIdTag())) {
                            var transactionId = (String) endpointHandler.get(this.eventTags.getEndpointHandlerTransactionIdTag());
                            addTransactionToDirectedGraph(transactionId, directedGraph, handledTransactions);
                        }
                    }
                }
            }
        }
        // Add edges within a transaction.
        var vertices = directedGraph.getVertices();
        for (var vertex : vertices) {
            if (!(vertex instanceof Event)) {
                continue;
            }
            if (!directedGraph.getAdjacentOutVertices(vertex).isEmpty()) {
                continue;
            }
            // We've found an event without a connection to a next vertex. Let's see if we can make some connections.
            var event = (Event) vertex;
            if (event.getTransactionId() != null) {
                var transactionEvents = directedGraph.findEvents(e -> event.getTransactionId().equals(e.getTransactionId()));
                transactionEvents.sort(Comparator.comparing(Event::getEventStartTime));
                for (var transactionEvent : transactionEvents) {
                    if (!transactionEvent.getEventStartTime().isBefore(event.getEventStartTime()) && !event.getEventId().equals(transactionEvent.getEventId())) {
                        directedGraph.addEdge(event, transactionEvent);
                        break;
                    }
                }
            }
        }
        // See if all requests are connected to responses. If not, try to add the connection.
        for (var vertex : vertices) {
            if (!(vertex instanceof Event)) {
                continue;
            }
            var event = (Event) vertex;
            // Check if this event is a response and has a correlation to a request.
            if (!event.isResponse() || event.getCorrelationEventId() == null) {
                continue;
            }
            // Find the corresponding request of the response.
            Optional<Event> first = vertices.stream().filter(v -> v instanceof Event).map(e -> (Event) e).filter(e -> e.getEventId().equals(event.getCorrelationEventId())).findFirst();
            if (first.isEmpty()) {
                continue;
            }
            var requestEvent = first.get();
            if (directedGraph.hasPathTo(requestEvent, event)) {
                // There's a path from the request to the response. Nothing to do...
                continue;
            }
            // Let's see if we can find the end of the path seen from the request.
            Vertex lastAfterRequest = requestEvent;
            while (directedGraph.getAdjacentOutVertices(lastAfterRequest) != null && !directedGraph.getAdjacentOutVertices(lastAfterRequest).isEmpty()) {
                var next = directedGraph.getAdjacentOutVertices(lastAfterRequest).get(0);
                if (next.equals(requestEvent)) {
                    // Somehow a cycle has introduced into the DAG. This should not be possible.
                    break;
                }
                lastAfterRequest = next;
            }
            if (lastAfterRequest.equals(requestEvent)) {
                // Nothing found...
                continue;
            }
            Vertex firstBeforeResponse = event;
            while (directedGraph.getAdjacentInVertices(firstBeforeResponse) != null && !directedGraph.getAdjacentInVertices(firstBeforeResponse).isEmpty()) {
                var next = directedGraph.getAdjacentInVertices(firstBeforeResponse).get(0);
                if (next.equals(event)) {
                    // Somehow a cycle has introduced into the DAG. This should not be possible.
                    break;
                }
                firstBeforeResponse = next;
            }
            directedGraph.addEdge(lastAfterRequest, firstBeforeResponse);
        }
        return directedGraph.calculateAbsoluteMetrics();
    }

    /**
     * Adds a transaction to a <code>DirectedGraph</code>.
     *
     * @param transactionId       The id of the transaction to add.
     * @param directedGraph       The <code>DirectedGraph</code> to add the transaction data to.
     * @param handledTransactions A set of transaction id's on which the recursive transaction id's that are covered by this method are added.
     */
    private void addTransactionToDirectedGraph(String transactionId, DirectedGraph directedGraph, Set<String> handledTransactions) {
        if (handledTransactions.contains(transactionId)) {
            return;
        }
        handledTransactions.add(transactionId);
        BoolQueryBuilder findEventsQuery = new BoolQueryBuilder()
                .must(new TermQueryBuilder(this.eventTags.getEndpointsTag() +
                        "." + this.eventTags.getEndpointHandlersTag() +
                        "." + this.eventTags.getEndpointHandlerTransactionIdTag() + KEYWORD_SUFFIX, transactionId)
                ).filter(QueryBuilders.termsQuery(this.eventTags.getObjectTypeTag(), "http", "messaging"));
        // No principal filtered query. We would like to show the entire event chain, but the user should not be able to retrieve all information.
        SearchRequestBuilder searchRequest = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL))
                .setQuery(findEventsQuery)
                .setSort(SortBuilders.fieldSort("_doc"))
                .setFetchSource(null, this.eventTags.getPayloadTag());
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequest);
        if (!scrollableSearch.hasNext()) {
            return;
        }
        Set<String> transactionIds = new HashSet<>();
        for (SearchHit searchHit : scrollableSearch) {
            transactionIds.addAll(addSearchHitToDirectedGraph(searchHit, directedGraph));
        }
        transactionIds.forEach(c -> addTransactionToDirectedGraph(c, directedGraph, handledTransactions));
    }

    /**
     * Add a <code>SearchHit</code> to the given <code>DirectedGraph</code>. When a <code>SearchHit</code> is already present in the <code>DirectedGraph</code> this method call is ignored.
     *
     * @param searchHit     The <code>SearchHit</code> to add.
     * @param directedGraph The <code>DirectedGraph</code> to add the <code>SearchHit</code> to.
     * @return A <code>Set</code> of transaction id's that are stored within this <code>SearchHit</code>. This list can be used to create a full event chain.
     */
    private Set<String> addSearchHitToDirectedGraph(SearchHit searchHit, DirectedGraph directedGraph) {
        if (directedGraph.containsEvent(e -> searchHit.getId().equals(e.getEventId()))) {
            return Collections.emptySet();
        }
        var transactionIds = new HashSet<String>();
        var source = searchHit.getSourceAsMap();
        var expiry = getInstant(this.eventTags.getExpiryTag(), source);
        String subType = null;
        if ("http".equals(getString(this.eventTags.getObjectTypeTag(), source))) {
            subType = getString(this.eventTags.getHttpEventTypeTag(), source);
        } else if ("messaging".equals(getString(this.eventTags.getObjectTypeTag(), source))) {
            subType = getString(this.eventTags.getMessagingEventTypeTag(), source);
        }
        var eventName = getString(this.eventTags.getNameTag(), source);
        var response = false;
        String correlationId = null;
        if (MessagingTelemetryEvent.MessagingEventType.RESPONSE.name().equals(subType) || HttpTelemetryEvent.HttpEventType.RESPONSE.name().equals(subType)) {
            response = true;
            correlationId = getString(this.eventTags.getCorrelationIdTag(), source);
        }
        var async = MessagingTelemetryEvent.MessagingEventType.FIRE_FORGET.name().equals(subType);
        var writerEndpoints = new ArrayList<Endpoint>();
        var readerEndpoints = new ArrayList<Endpoint>();
        List<Map<String, Object>> endpoints = getArray(this.eventTags.getEndpointsTag(), source, Collections.emptyList());
        for (var endpointValues : endpoints) {
            var endpoint = new Endpoint(UUID.randomUUID().toString(), getString(this.eventTags.getEndpointNameTag(), endpointValues)).setEventId(searchHit.getId());
            if ("http".equals(getString(this.eventTags.getObjectTypeTag(), source)) && eventName == null) {
                eventName = subType + " " + endpoint.getName();
            }
            List<Map<String, Object>> endpointHandlers = getArray(this.eventTags.getEndpointHandlersTag(), endpointValues);
            if (endpointHandlers != null) {
                for (var eh : endpointHandlers) {
                    var writer = EndpointHandler.EndpointHandlerType.WRITER.name().equals(getString(this.eventTags.getEndpointHandlerTypeTag(), eh));
                    var transactionId = getString(this.eventTags.getEndpointHandlerTransactionIdTag(), eh);
                    if (transactionId != null) {
                        transactionIds.add(transactionId);
                    }
                    var startTime = getInstant(this.eventTags.getEndpointHandlerHandlingTimeTag(), eh);
                    String appName = null;
                    String appInstance = null;
                    Map<String, Object> appMap = getObject(this.eventTags.getEndpointHandlerApplicationTag(), eh);
                    if (appMap != null) {
                        appName = getString(this.eventTags.getApplicationNameTag(), appMap);
                        appInstance = getString(this.eventTags.getApplicationInstanceTag(), appMap);
                    }
                    Instant endTime = null;
                    if (!response) {
                        var responseTime = getLong(this.eventTags.getEndpointHandlerResponseTimeTag(), eh);
                        if (responseTime != null) {
                            endTime = startTime.plusMillis(responseTime);
                        } else if (expiry != null) {
                            endTime = expiry;
                        }
                    }
                    Application app = null;
                    if (appName != null) {
                        var appKey = appInstance == null ? appName : appName + " (" + appInstance + ")";
                        app = new Application(appKey, appName).setInstance(appInstance);
                    }
                    Event event = new Event(searchHit.getId(), eventName, app)
                            .setTransactionId(transactionId)
                            .setCorrelationEventId(correlationId)
                            .setEventStartTime(startTime)
                            .setEventEndTime(endTime)
                            .setAsync(async)
                            .setResponse(response)
                            .setSent(writer)
                            .setEndpointName(endpoint.getName())
                            .setOrder(writer ? 0 : 1);
                    if (writer) {
                        directedGraph.addEdge(event, endpoint);
                        writerEndpoints.add(endpoint);
                    } else {
                        directedGraph.addEdge(endpoint, event);
                        readerEndpoints.add(endpoint);
                    }
                }
            }
        }
        // Check if we have a situation where we have endpoints with a single write and no readers. Try to match them with endpoints without a writer and only readers.
        for (var writerEndpoint : writerEndpoints) {
            if (!readerEndpoints.contains(writerEndpoint)) {
                // Filter all reader endpoints without a corresponding writer endpoint
                Optional<Endpoint> first = readerEndpoints.stream().filter(e -> !writerEndpoints.contains(e)).findFirst();
                if (first.isPresent()) {
                    directedGraph.addEdge(writerEndpoint, first.get());
                    readerEndpoints.add(first.get());
                }
            }
        }
        return transactionIds;
    }

    @SuppressWarnings("unchecked")
    private boolean isWithinTransaction(Map<String, Object> endpointHandler, String id) {
        if (endpointHandler == null) {
            return false;
        }
        String transactionId = (String) endpointHandler.get(this.eventTags.getEndpointHandlerTransactionIdTag());
        if (!id.equals(transactionId)) {
            return false;
        }
        Map<String, Object> application = (Map<String, Object>) endpointHandler.get(this.eventTags.getEndpointHandlerApplicationTag());
        return application != null;
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
            addStringElementToJsonBuffer("id", searchHit.getId(), result, false);
            result.append(", \"source\": ").append(searchHit.getSourceAsString());
            result.append("}");
        }
    }
}
