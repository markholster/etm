/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.gui.rest.services.search;

import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.gui.rest.export.FileType;
import com.jecstar.etm.gui.rest.export.QueryExporter;
import com.jecstar.etm.gui.rest.services.AbstractIndexMetadataService;
import com.jecstar.etm.gui.rest.services.Keyword;
import com.jecstar.etm.gui.rest.services.dashboard.domain.GraphContainer;
import com.jecstar.etm.gui.rest.services.search.graphs.*;
import com.jecstar.etm.gui.rest.services.search.query.AdditionalQueryParameter;
import com.jecstar.etm.gui.rest.services.search.query.EtmQuery;
import com.jecstar.etm.gui.rest.services.search.query.Field;
import com.jecstar.etm.gui.rest.services.search.query.ResultLayout;
import com.jecstar.etm.gui.rest.services.search.query.converter.EtmQueryConverter;
import com.jecstar.etm.server.core.EtmException;
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
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
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
    private final EtmQueryConverter etmQueryConverter = new EtmQueryConverter();
    private final String transactionIdPath = eventTags.getEndpointsTag() + "." + eventTags.getEndpointHandlersTag() + "." + eventTags.getEndpointHandlerTransactionIdTag();

    public static void initialize(DataRepository dataRepository, EtmConfiguration etmConfiguration) {
        SearchService.dataRepository = dataRepository;
        SearchService.etmConfiguration = etmConfiguration;
        SearchService.requestEnhancer = new RequestEnhancer(etmConfiguration);
    }

    @GET
    @Path("/userdata")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String getUserData() {
        var etmPrincipal = getEtmPrincipal();
        var builder = new JsonBuilder();
        List<Keyword> keywords = getIndexFields(SearchService.dataRepository, ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL);
        builder.startObject();
        builder.field("max_number_of_search_templates", etmConfiguration.getMaxSearchTemplateCount());
        builder.field("max_number_of_historical_queries", Math.min(etmPrincipal.getHistorySize(), etmConfiguration.getMaxSearchHistoryCount()));
        builder.field("max_number_of_events_in_download", etmConfiguration.getMaxSearchResultDownloadRows());
        builder.field("timeZone", etmPrincipal.getTimeZone().getID());
        builder.startArray("keywords");
        builder.startObject();
        builder.field("index", ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL);
        builder.startArray("keywords");
        for (var keyword : keywords) {
            builder.startObject();
            builder.field("name", keyword.getName());
            builder.field("type", keyword.getType());
            builder.field("date", keyword.isDate());
            builder.field("number", keyword.isNumber());
            builder.endObject();
        }
        builder.endArray();
        builder.endObject();
        builder.endArray(); //End  Keywords

        var userData = getCurrentUser(
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + ".search_templates",
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + ".additional_query_parameters",
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.configurationTags.getSearchHistoryTag()
        );
        builder.rawField("search_templates", toString(userData.get("search_templates")));
        if (userData.containsKey("additional_query_parameters")) {
            builder.rawField("additional_query_parameters", toString(userData.get("additional_query_parameters")));
        }
        builder.rawField(this.configurationTags.getSearchHistoryTag(), toString(userData.get(this.configurationTags.getSearchHistoryTag())));
        builder.endObject();
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    @POST
    @Path("/distinctvalues")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String getDistinctValues(String json) {
        var etmPrincipal = getEtmPrincipal();
        var fieldsMap = (List<Map<String, Object>>) getArray("fields", toMap(json));
        var params = new ArrayList<AdditionalSearchParameter>();
        var directedGraph = new DirectedGraph<AdditionalSearchParameter>();
        for (int i = 0; i < fieldsMap.size(); i++) {
            Map<String, Object> fieldMap = fieldsMap.get(i);
            AdditionalSearchParameter additionalSearchParameter = new AdditionalSearchParameter(i);
            additionalSearchParameter.setField(getString("field", fieldMap));
            additionalSearchParameter.setType(getString("type", fieldMap));
            additionalSearchParameter.setValue(fieldMap.get("value"));
            if (AdditionalQueryParameter.FieldType.SELECT.name().equals(additionalSearchParameter.getType()) || AdditionalQueryParameter.FieldType.SELECT_MULTI.name().equals(additionalSearchParameter.getType())) {
                directedGraph.addVertex(additionalSearchParameter);
            }
            params.add(additionalSearchParameter);
        }

        for (int i = 0; i < fieldsMap.size(); i++) {
            Map<String, Object> fieldMap = fieldsMap.get(i);
            final int index = i;
            var relatesToList = (List<Integer>) fieldMap.get("relates_to");
            if (relatesToList != null && !relatesToList.isEmpty()) {
                relatesToList.forEach(c -> directedGraph.addEdge(params.get(index), params.get(c)));
            }
        }

        List<AdditionalSearchParameter> directedAcyclicOrder = directedGraph.getDirectedAcyclicOrder();
        Collections.reverse(directedAcyclicOrder);
        for (AdditionalSearchParameter vertex : directedAcyclicOrder) {
            if (!AdditionalQueryParameter.FieldType.SELECT.name().equals(vertex.getType()) && !AdditionalQueryParameter.FieldType.SELECT_MULTI.name().equals(vertex.getType())) {
                continue;
            }
            BoolQueryBuilder rootQuery = new BoolQueryBuilder();
            addFilterQuery(etmPrincipal, rootQuery);

            for (var filterVertex : directedGraph.getAdjacentOutVertices(vertex)) {
                QueryBuilder filterQuery = createFilterQueryBuilder(filterVertex.getField(), filterVertex.getType(), filterVertex.getValue(), etmPrincipal);
                if (filterQuery != null) {
                    rootQuery.must(filterQuery);
                }
            }
            var termField = getFieldData(dataRepository, ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL, vertex.getField());
            if (termField == null) {
                continue;
            }
            var builder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL))
                    .setSize(0)
                    .setFetchSource(false)
                    .setQuery(rootQuery)
                    .addAggregation(
                            AggregationBuilders.terms("distinct")
                                    .field(termField.sortProperty)
                                    .order(BucketOrder.key(true))
                                    .size(1000)
                    );
            var searchResponse = dataRepository.search(builder);
            var distinct = (ParsedTerms) searchResponse.getAggregations().get("distinct");
            var distinctValues = distinct.getBuckets().stream().map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList());
            if (vertex.hasValue()) {
                if (vertex.getValue() instanceof Collection<?>) {
                    ((Collection<?>) vertex.getValue()).removeIf(o -> !distinctValues.contains(o));
                } else if (!distinctValues.contains(vertex.getValue())) {
                    vertex.setValue(null);
                }
            }
            vertex.addDistinctValues(distinctValues);
        }


        JsonBuilder jsonBuilder = new JsonBuilder();
        jsonBuilder.startObject();
        jsonBuilder.startArray("fields");
        for (var vertex : directedAcyclicOrder) {
            jsonBuilder.startObject();
            jsonBuilder.field("id", vertex.getId());
            if (vertex.hasValue()) {
                if (vertex.getValue() instanceof Collection<?>) {
                    jsonBuilder.field("value", (Collection<?>) vertex.getValue());
                } else {
                    jsonBuilder.field("value", vertex.getValue().toString());
                }
            }
            jsonBuilder.field("distinct_values", vertex.getDistinctValues());
            jsonBuilder.endObject();
        }
        jsonBuilder.endArray();
        jsonBuilder.endObject();
        return jsonBuilder.build();
    }

    @PUT
    @Path("/templates/{templateName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String addSearchTemplate(@PathParam("templateName") String templateName, String json) {
        EtmQuery query = this.etmQueryConverter.read(json);
        query.setName(templateName);

        Map<String, Object> userData = getCurrentUser(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + ".search_templates");
        List<Map<String, Object>> currentSearchTemplates = new ArrayList<>();
        if (userData.containsKey("search_templates")) {
            currentSearchTemplates = getArray("search_templates", userData, new ArrayList<>());
        }
        ListIterator<Map<String, Object>> iterator = currentSearchTemplates.listIterator();
        boolean updated = false;
        while (iterator.hasNext()) {
            Map<String, Object> searchtemplateMap = iterator.next();
            if (templateName.equals(getString("name", searchtemplateMap))) {
                iterator.set(toMap(this.etmQueryConverter.write(query)));
                updated = true;
                break;
            }
        }
        if (!updated) {
            if (currentSearchTemplates.size() >= etmConfiguration.getMaxSearchTemplateCount()) {
                throw new EtmException(EtmException.MAX_NR_OF_SEARCH_TEMPLATES_REACHED);
            }
            currentSearchTemplates.add(toMap(this.etmQueryConverter.write(query)));
        }
        Map<String, Object> source = new HashMap<>();
        source.put("search_templates", currentSearchTemplates);
        updateCurrentUser(source, false);
        return "{\"status\":\"success\"}";
    }

    @DELETE
    @Path("/templates/{templateName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String removeSearchTemplate(@PathParam("templateName") String templateName) {
        Map<String, Object> userData = getCurrentUser(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + ".search_templates");

        if (userData.isEmpty()) {
            return "{\"status\":\"success\"}";
        }
        List<Map<String, Object>> currentSearchTemplates = new ArrayList<>();
        if (userData.containsKey("search_templates")) {
            currentSearchTemplates = getArray("search_templates", userData);
        }
        ListIterator<Map<String, Object>> iterator = currentSearchTemplates.listIterator();
        while (iterator.hasNext()) {
            Map<String, Object> searchTemplateValues = iterator.next();
            if (templateName.equals(getString(GraphContainer.NAME, searchTemplateValues))) {
                iterator.remove();
                break;
            }
        }

        Map<String, Object> source = new HashMap<>();
        // Prepare new source map with the remaining search templates.
        source.put("search_templates", currentSearchTemplates);
        updateCurrentUser(source, false);
        return "{\"status\":\"success\"}";

    }

    @PUT
    @Path("/additionalparameters")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String addAdditionalSearchParameters(String json) {
        var requestValues = toMap(json);
        var objectMap = new HashMap<String, Object>();
        var paramsMap = new HashMap<String, Object>();
        paramsMap.put("additional_query_parameters", requestValues.get("params"));
        objectMap.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, paramsMap);
        var builder = requestEnhancer.enhance(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + getEtmPrincipal().getId())
        ).setDoc(objectMap).setDocAsUpsert(true);
        dataRepository.update(builder);
        return "{ \"status\": \"success\" }";
    }

    @POST
    @Path("/query")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String executeQuery(String json) {
        var startTime = System.currentTimeMillis();
        var etmPrincipal = getEtmPrincipal();
        var payloadVisible = etmPrincipal.isInAnyRole(SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WRITE);

        var now = Instant.now();
        var auditLogBuilder = new QueryAuditLogBuilder().setTimestamp(now).setHandlingTime(now).setPrincipalId(etmPrincipal.getId());

        var etmQuery = this.etmQueryConverter.read(json);
        var requestBuilder = createRequestFromInput(etmQuery, etmPrincipal);
        var numberFormat = NumberFormat.getInstance(etmPrincipal.getLocale());
        var response = dataRepository.search(requestBuilder);
        var builder = new JsonBuilder();
        builder.startObject();
        builder.field("status", "success");
        if (response.getShardFailures() != null && response.getShardFailures().length > 0) {
            try {
                var contentBuilder = XContentFactory.jsonBuilder();
                contentBuilder.startObject();
                response.getShardFailures()[0].toXContent(contentBuilder, ToXContent.EMPTY_PARAMS);
                contentBuilder.endObject();
                var errorMap = toMap(Strings.toString(contentBuilder));
                var reason = getObject("reason", errorMap);
                if (reason != null) {
                    builder.field("warning", getString("reason", reason, "unknown error"));
                }
            } catch (IOException e) {
                if (log.isWarningLevelEnabled()) {
                    log.logWarningMessage(e.getMessage(), e);
                }
            }

        }
        builder.field("hits", response.getHits().getTotalHits().value);
        builder.field("hits_relation", response.getHits().getTotalHits().relation.name());
        builder.field("hits_as_string", numberFormat.format(response.getHits().getTotalHits().value) + (TotalHits.Relation.GREATER_THAN_OR_EQUAL_TO.equals(response.getHits().getTotalHits().relation) ? "+" : ""));
        builder.field("start_ix", etmQuery.getResultLayout().getStartIndex());
        builder.field("end_ix", etmQuery.getResultLayout().getStartIndex() + response.getHits().getHits().length - 1);
        builder.field("has_more_results", etmQuery.getResultLayout().getStartIndex() + response.getHits().getHits().length < response.getHits().getTotalHits().value - 1);
        builder.field("time_zone", etmPrincipal.getTimeZone().getID());
        builder.field("may_see_payload", payloadVisible);
        builder.startArray("results");
        addSearchHits(builder, response.getHits());
        builder.endArray();
        long queryTime = System.currentTimeMillis() - startTime;
        builder.field("query_time", queryTime);
        builder.field("query_time_as_string", numberFormat.format(queryTime));
        builder.endObject();

        if (etmQuery.getResultLayout().getStartIndex() == 0) {
            writeQueryHistory(
                    etmQuery,
                    Math.min(etmPrincipal.getHistorySize(), etmConfiguration.getMaxSearchHistoryCount())
            );
            // Log the query request to the audit logs.
            String executedQuery = null;
            try {
                var contentBuilder = XContentFactory.jsonBuilder();
                requestBuilder.build().source().query().toXContent(contentBuilder, ToXContent.EMPTY_PARAMS);
                executedQuery = Strings.toString(contentBuilder);
            } catch (IOException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage(e.getMessage(), e);
                }
            }
            auditLogBuilder
                    .setUserQuery(etmQuery.getResultLayout().getQuery())
                    .setExectuedQuery(executedQuery)
                    .setNumberOfResults(response.getHits().getTotalHits().value)
                    .setNumberOfResultsRelation(response.getHits().getTotalHits().relation.name())
                    .setQueryTime(queryTime);
            IndexRequestBuilder indexRequestBuilder = requestEnhancer.enhance(
                    new IndexRequestBuilder(ElasticsearchLayout.AUDIT_LOG_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(now))
            )
                    .setSource(this.queryAuditLogConverter.write(auditLogBuilder.build()), XContentType.JSON);
            dataRepository.indexAsync(indexRequestBuilder, DataRepository.noopActionListener());
        }
        return builder.build();
    }

    private SearchRequestBuilder createRequestFromInput(EtmQuery etmQuery, EtmPrincipal etmPrincipal) {
        EtmQueryBuilder etmQueryBuilder = new EtmQueryBuilder(etmQuery.getResultLayout().getQuery(), dataRepository, SearchService.requestEnhancer)
                .setTimeZone(etmPrincipal.getTimeZone().getID());
        List<AdditionalQueryParameter> filterFields = etmQuery.getAdditionalQueryParameters();
        for (var filterField : filterFields) {
            QueryBuilder filterQuery = createFilterQueryBuilder(filterField.getField(), filterField.getFieldType().name(), filterField.getValue(), etmPrincipal);
            if (filterQuery != null) {
                etmQueryBuilder.filterRoot(filterQuery);
            }
        }
        var rangeQueryFilter = new RangeQueryBuilder("timestamp").lte(etmQuery.getResultLayout().getTimestamp());
        etmQueryBuilder.filterRoot(rangeQueryFilter).filterJoin(rangeQueryFilter);
        var requestedFields = etmQuery.getResultLayout().getFieldNames();
        if (!requestedFields.contains(this.transactionIdPath)) {
            requestedFields.add(this.transactionIdPath);
        }
        var requestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(etmConfiguration.mergeRemoteIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL)))
                .setQuery(addFilterQuery(getEtmPrincipal(), etmQueryBuilder.buildRootQuery()))
                .setFetchSource(requestedFields.toArray(new String[0]), null)
                .setFrom(etmQuery.getResultLayout().getStartIndex())
                .setSize(Math.min(etmQuery.getResultLayout().getMaxResults(), 500));
        if (etmQuery.getResultLayout().getSortField() != null && etmQuery.getResultLayout().getSortField().trim().length() > 0) {
            var fieldData = getFieldData(dataRepository, ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL, etmQuery.getResultLayout().getSortField());
            if (fieldData != null) {
                requestBuilder.setSort(fieldData.sortProperty, ResultLayout.SortOrder.DESC.equals(etmQuery.getResultLayout().getSortOrder()) ? SortOrder.DESC : SortOrder.ASC);
            }
        }
        return requestBuilder;
    }

    private QueryBuilder createFilterQueryBuilder(String field, String fieldType, Object fieldValue, EtmPrincipal etmPrincipal) {
        if (fieldValue == null || (fieldValue instanceof Collection<?> && ((Collection<?>) fieldValue).isEmpty())) {
            return null;
        }
        var fieldData = getFieldData(dataRepository, ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL, field);
        if (fieldData == null) {
            return null;
        }
        if (AdditionalQueryParameter.FieldType.RANGE_START.name().equals(fieldType)) {
            var rangeFilter = QueryBuilders.rangeQuery(fieldData.sortProperty);
            if (fieldData.isDate()) {
                var instant = DateUtils.parseDateString(fieldValue.toString(), etmPrincipal.getTimeZone().toZoneId(), true);
                if (instant != null) {
                    rangeFilter.gte("" + instant.toEpochMilli());
                } else {
                    rangeFilter.gte(fieldValue.toString());
                }
                rangeFilter.timeZone(etmPrincipal.getTimeZone().getID());
            } else {
                rangeFilter.gte(fieldValue);
            }
            return rangeFilter;
        } else if (AdditionalQueryParameter.FieldType.RANGE_END.name().equals(fieldType)) {
            var rangeFilter = QueryBuilders.rangeQuery(fieldData.sortProperty);
            if (fieldData.isDate()) {
                var instant = DateUtils.parseDateString(fieldValue.toString(), etmPrincipal.getTimeZone().toZoneId(), false);
                if (instant != null) {
                    rangeFilter.lte("" + instant.toEpochMilli());
                } else {
                    rangeFilter.lte(fieldValue.toString());
                }
                rangeFilter.timeZone(etmPrincipal.getTimeZone().getID());
            } else {
                rangeFilter.lte(fieldValue);
            }
            return rangeFilter;
        } else {
            if (fieldValue instanceof Collection) {
                return QueryBuilders.termsQuery(fieldData.sortProperty, (Collection<?>) fieldValue);
            } else {
                return QueryBuilders.termQuery(fieldData.sortProperty, fieldValue);
            }
        }
    }

    private void writeQueryHistory(EtmQuery etmQuery, int maxHistorySize) {
        Map<String, Object> userData = getCurrentUser(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.configurationTags.getSearchHistoryTag());
        List<Map<String, Object>> currentSearchHistories = new ArrayList<>();
        if (userData.containsKey(this.configurationTags.getSearchHistoryTag())) {
            currentSearchHistories = getArray("search_history", userData, new ArrayList<>());
        }
        int ixOfQuery = -1;
        for (int i = 0; i < currentSearchHistories.size(); i++) {
            var searchHistory = currentSearchHistories.get(i);
            var resultLayout = getObject(EtmQuery.RESULT_LAYOUT, searchHistory);
            if (resultLayout == null) {
                continue;
            }
            if (etmQuery.getResultLayout().getQuery().equals(getString("current_query", resultLayout))) {
                ixOfQuery = i;
                break;
            }
        }
        if (ixOfQuery >= 0) {
            currentSearchHistories.remove(ixOfQuery);
        }
        var origTimestamp = etmQuery.getResultLayout().getTimestamp();
        currentSearchHistories.add(0, toMap(this.etmQueryConverter.write(etmQuery)));
        etmQuery.getResultLayout().setTimestamp(origTimestamp);
        if (currentSearchHistories.size() > maxHistorySize) {
            currentSearchHistories = currentSearchHistories.subList(0, maxHistorySize);
        }
        Map<String, Object> source = new HashMap<>();
        source.put(this.configurationTags.getSearchHistoryTag(), currentSearchHistories);
        updateCurrentUser(source, true);
    }


    @GET
    @Path("/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public Response getDownload(@QueryParam("q") String json) {
        var etmPrincipal = getEtmPrincipal();
        var valueMap = toMap(json);
        var etmQuery = this.etmQueryConverter.read(valueMap);
        if (getBoolean("includePayload", valueMap)
                && !etmQuery.getResultLayout().getFieldNames().contains(this.eventTags.getPayloadTag())) {
            Field field = new Field().setName("Payload").setField("payload").setFormat(Field.Format.PLAIN).setArraySelector(Field.ArraySelector.FIRST);
            etmQuery.getResultLayout().addField(field, etmPrincipal);

        }
        SearchRequestBuilder requestBuilder = createRequestFromInput(etmQuery, etmPrincipal);

        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, requestBuilder, etmQuery.getResultLayout().getStartIndex());
        FileType fileType = FileType.valueOf(getString("fileType", valueMap).toUpperCase());
        File result = new QueryExporter().exportToFile(
                scrollableSearch,
                fileType,
                Math.min(etmQuery.getResultLayout().getMaxResults(),
                        etmConfiguration.getMaxSearchResultDownloadRows()),
                etmPrincipal,
                etmQuery.getResultLayout().toFieldLayouts(),
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
        var now = Instant.now();
        var payloadVisible = getEtmPrincipal().maySeeEventPayload();
        var auditLogBuilder = new GetEventAuditLogBuilder()
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
        var builder = new JsonBuilder();
        builder.startObject();
        builder.field("time_zone", getEtmPrincipal().getTimeZone().getID());
        SearchHit searchHit = getEvent(eventId, new String[]{"*"}, !payloadVisible ? new String[]{this.eventTags.getPayloadTag()} : null);
        if (searchHit != null) {
            auditLogBuilder.setFound(true);
            Map<String, Object> valueMap = searchHit.getSourceAsMap();
            builder.startObject("event");
            builder.field("index", searchHit.getIndex());
            builder.field("id", searchHit.getId());
            builder.rawField("source", toString(valueMap));
            builder.endObject();
            // Add the name to the audit log.
            auditLogBuilder.setEventName(getString(this.eventTags.getNameTag(), valueMap));
            // Try to find an event this event is correlating to.
            String correlatedToId = getString(this.eventTags.getCorrelationIdTag(), valueMap);
            boolean correlationAdded = false;
            if (correlatedToId != null && !correlatedToId.equals(eventId)) {
                SearchHit correlatedEvent = conditionallyGetEvent(correlatedToId, payloadVisible);
                if (correlatedEvent != null) {
                    builder.startArray("correlated_events");
                    builder.startObject();
                    builder.field("index", correlatedEvent.getIndex());
                    builder.field("id", correlatedEvent.getId());
                    builder.rawField("source", correlatedEvent.getSourceAsString());
                    builder.endObject();
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
                            builder.startArray("correlated_events");
                        }
                        builder.startObject();
                        builder.field("index", correlatedEvent.getIndex());
                        builder.field("id", correlatedEvent.getId());
                        builder.rawField("source", correlatedEvent.getSourceAsString());
                        builder.endObject();
                        correlationAdded = true;
                        auditLogBuilder.addCorrelatedEvent(correlatedEvent.getId());
                    }
                }
            }
            if (correlationAdded) {
                builder.endArray();
            }
            if (auditLogListener != null) {
                // Audit logs received async. Wait here to add the to the result.
                SearchResponse auditLogResponse = auditLogListener.get();
                if (auditLogResponse != null && auditLogResponse.getHits().getHits().length != 0) {
                    builder.startArray("audit_logs");
                    for (SearchHit hit : auditLogResponse.getHits().getHits()) {
                        Map<String, Object> auditLogValues = hit.getSourceAsMap();
                        builder.startObject();
                        builder.field("direct", getString(GetEventAuditLog.EVENT_ID, auditLogValues).equals(eventId));
                        builder.field("handling_time", getLong(AuditLog.HANDLING_TIME, auditLogValues));
                        builder.field("principal_id", getString(AuditLog.PRINCIPAL_ID, auditLogValues));
                        builder.field("payload_visible", getBoolean(GetEventAuditLog.PAYLOAD_VISIBLE, auditLogValues, true));
                        builder.field("downloaded", getBoolean(GetEventAuditLog.DOWNLOADED, auditLogValues, false));
                        builder.endObject();
                    }
                    builder.endArray();
                }
            }
        }
        builder.endObject();
        // Log the retrieval request to the audit logs.
        IndexRequestBuilder indexRequestBuilder = requestEnhancer.enhance(
                new IndexRequestBuilder(ElasticsearchLayout.AUDIT_LOG_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(now))
        )
                .setSource(this.getEventAuditLogConverter.write(auditLogBuilder.build()), XContentType.JSON);
        dataRepository.indexAsync(indexRequestBuilder, DataRepository.noopActionListener());
        return builder.build();
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
        var builder = new JsonBuilder();
        builder.startObject();
        builder.field("locale", getLocalFormatting(getEtmPrincipal()));
        builder.startArray("layers");
        for (var layer : layers) {
            builder.startObject();
            builder.startArray("vertices");
            for (var vertex : layer.getVertices()) {
                builder.startObject();
                vertex.toJson(builder);
                var childVertices = layer.getChildVertices(vertex);
                if (childVertices != null && childVertices.size() > 0) {
                    builder.startArray("children");
                    for (var childVertex : childVertices) {
                        builder.startObject();
                        childVertex.toJson(builder);
                        builder.endObject();
                    }
                    builder.endArray();
                }
                builder.endObject();
            }
            builder.endArray().endObject();
        }
        builder.endArray();
        builder.startArray("edges");
        Duration totalEventTime = directedGraph.getTotalEventTime();
        if (totalEventTime != null && totalEventTime.toMillis() <= 0) {
            totalEventTime = null;
        }
        for (var vertex : directedGraph.getVertices()) {
            for (var adjVertex : directedGraph.getAdjacentOutVertices(vertex)) {
                builder.startObject();
                builder.field("from", vertex.getVertexId());
                builder.field("to", adjVertex.getVertexId());
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
                        builder.field("transition_time", transitionTime);
                        builder.field("transition_time_percentage", (double) ((float) transitionTime / (float) totalEventTime.toMillis()));
                    }
                }
                builder.endObject();
            }
        }
        builder.endArray().endObject();
        return builder.build();

    }

    @GET
    @Path("/event/{id}/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE})
    public String getEventChainEndpoint(@PathParam("id") String eventId) {
        var builder = new JsonBuilder();
        builder.startObject();
        builder.field("time_zone", getEtmPrincipal().getTimeZone().getID());
        SearchHit searchHit = getEvent(eventId, new String[]{this.eventTags.getEndpointsTag() + ".*"}, null);
        if (searchHit != null) {
            builder.startObject("event");
            builder.field("index", searchHit.getIndex());
            builder.field("id", searchHit.getId());
            builder.rawField("source", searchHit.getSourceAsString());
            builder.endObject();
        }
        builder.endObject();
        return builder.build();
    }

    private SearchHit getEvent(String eventId, String[] includes, String[] excludes) {
        IdsQueryBuilder idsQueryBuilder = new IdsQueryBuilder()
                .addIds(eventId);
        BoolQueryBuilder query = new BoolQueryBuilder();
        query.must(idsQueryBuilder);
        SearchRequestBuilder builder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(etmConfiguration.mergeRemoteIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL)))
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
        SearchRequestBuilder builder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(etmConfiguration.mergeRemoteIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL)))
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
        var events = getTransactionEvents(transactionId);
        if (events == null) {
            return null;
        }
        var builder = new JsonBuilder();
        builder.startObject();
        builder.field("time_zone", getEtmPrincipal().getTimeZone().getID());
        builder.startArray("events");
        for (TransactionEvent event : events) {
            builder.startObject();
            builder.field("index", event.index);
            builder.field("object_type", event.objectType);
            builder.field("sub_type", event.subtype);
            builder.field("id", event.id);
            builder.field("handling_time", event.handlingTime);
            builder.field("name", event.name);
            builder.field("direction", event.direction);
            builder.field("payload", event.payload);
            builder.field("endpoint", event.endpoint);
            builder.endObject();
        }
        builder.endArray().endObject();
        return builder.build();
    }

    private List<TransactionEvent> getTransactionEvents(String transactionId) {
        BoolQueryBuilder findEventsQuery = new BoolQueryBuilder()
                .must(new TermQueryBuilder(this.eventTags.getEndpointsTag() +
                        "." + this.eventTags.getEndpointHandlersTag() +
                        "." + this.eventTags.getEndpointHandlerTransactionIdTag() + KEYWORD_SUFFIX, transactionId)
                );
        SearchRequestBuilder searchRequest = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(etmConfiguration.mergeRemoteIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL)))
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
        var events = directedGraph.findEvents(x -> !x.isResponse());
        events.sort(Comparator.comparing(Event::getEventStartTime).thenComparing(Event::getOrder).thenComparing(Event::getEventEndTime, Comparator.reverseOrder()));

        var etmPrincipal = getEtmPrincipal();
        var builder = new JsonBuilder();
        builder.startObject()
                .rawField("locale", getLocalFormatting(etmPrincipal))
                .startObject("chart_config")
                .startObject("credits").field("enabled", false).endObject()
                .startObject("legend").field("enabled", false).endObject()
                .startObject("time").field("timezone", etmPrincipal.getTimeZone().toZoneId().toString()).endObject()
                .startObject("chart").field("type", "xrange").endObject()
                .startObject("title").field("text", "Event chain times").endObject()
                .startObject("xAxis").field("type", "datetime").endObject()
                .startObject("yAxis")
                .startObject("title").field("text", "Events").endObject()
                .field("reversed", true)
                .field("categories", events.stream().map(e -> e.getName() + (e.isSent() ? " (sent)" : " (received)")).collect(Collectors.toSet()))
                .endObject()
                .startArray("series")
                .startObject()
                .field("name", "Chain overview")
                .field("pointPadding", 0)
                .field("colorByPoint", false)
                .field("colorIndex", 7)
                .startArray("data");
        for (int i = 0; i < events.size(); i++) {
            var event = events.get(i);
            builder.startObject();
            builder.field("x", event.getEventStartTime());
            builder.field("x2", (event.getEventEndTime() != null ? event.getEventEndTime().toEpochMilli() : event.getEventStartTime().toEpochMilli() + 10));
            builder.field("y", i);
            builder.field("partialFill", (event.getAbsoluteTransactionPercentage() != null ? event.getAbsoluteTransactionPercentage().doubleValue() : 0.0));
            builder.startObject("dataLabels").field("enabled", event.getAbsoluteTransactionPercentage() != null).endObject();
            builder.field("event_time", event.getTotalEventTime() != null ? etmPrincipal.getNumberFormat().format(event.getTotalEventTime().toMillis()) : null);
            builder.field("event_absolute_time", event.getAbsoluteDuration() != null ? etmPrincipal.getNumberFormat().format(event.getAbsoluteDuration().toMillis()) : null);
            builder.field("event_id", event.getEventId());
            builder.field("endpoint", event.getEndpointName());
            builder.field("application", (event.getParent() != null ? event.getParent().getName() : null));
            builder.field("application_instance", (event.getParent() != null ? event.getParent().getInstance() : null));
            builder.field("transaction_id", event.getTransactionId());
            builder.endObject();
        }
        builder.endArray();
        builder.startObject("tooltip")
                .field("pointFormat", "Name: <b>{point.yCategory}</b><br/>Application: <b>{point.application}</b><br/>Application instance: <b>{point.application_instance}</b><br/>Endpoint: <b>{point.endpoint}</b><br/>Response time: <b>{point.event_time}ms</b><br/>Absolute time: <b>{point.event_absolute_time}ms</b><br/>")
                .endObject().endObject().endArray();
        builder.endObject().endObject();
        return builder.build();
    }

    /**
     * Creates a <code>DirectedGraph</code> for a given event.
     *
     * @param eventId The event id.
     * @return The <code>DirectedGraph</code> for the event.
     */
    @SuppressWarnings("unchecked")
    private DirectedGraph<AbstractVertex> calculateDirectedGraph(String eventId) {
        IdsQueryBuilder idsQueryBuilder = new IdsQueryBuilder()
                .addIds(eventId);
        // No principal filtered query. We would like to show the entire event chain, but the user should not be able to retrieve all information.
        SearchRequestBuilder builder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(etmConfiguration.mergeRemoteIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL)))
                .setQuery(idsQueryBuilder)
                .setFetchSource(null, this.eventTags.getPayloadTag())
                .setFrom(0)
                .setSize(1);
        SearchResponse response = dataRepository.search(builder);
        if (response.getHits().getHits().length == 0) {
            return null;
        }
        DirectedGraph<AbstractVertex> directedGraph = new DirectedGraph<>();
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
        directedGraph.finishGraph();
        return directedGraph.calculateAbsoluteMetrics();
    }

    /**
     * Adds a transaction to a <code>DirectedGraph</code>.
     *
     * @param transactionId       The id of the transaction to add.
     * @param directedGraph       The <code>DirectedGraph</code> to add the transaction data to.
     * @param handledTransactions A set of transaction id's on which the recursive transaction id's that are covered by this method are added.
     */
    private void addTransactionToDirectedGraph(String transactionId, DirectedGraph<AbstractVertex> directedGraph, Set<String> handledTransactions) {
        if (transactionId == null || handledTransactions.contains(transactionId)) {
            return;
        }
        handledTransactions.add(transactionId);
        BoolQueryBuilder findEventsQuery = new BoolQueryBuilder()
                .must(new TermQueryBuilder(this.eventTags.getEndpointsTag() +
                        "." + this.eventTags.getEndpointHandlersTag() +
                        "." + this.eventTags.getEndpointHandlerTransactionIdTag() + KEYWORD_SUFFIX, transactionId)
                ).filter(QueryBuilders.termsQuery(this.eventTags.getObjectTypeTag(), "http", "messaging"));
        // No principal filtered query. We would like to show the entire event chain, but the user should not be able to retrieve all information.
        SearchRequestBuilder searchRequest = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(etmConfiguration.mergeRemoteIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL)))
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
    private Set<String> addSearchHitToDirectedGraph(SearchHit searchHit, DirectedGraph<AbstractVertex> directedGraph) {
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

    /**
     * Loads the current calling user from Elasticsearch.
     *
     * @param attributes The attributes to be loaded.
     * @return A <code>Map</code> containing the requested attributes. The user namespace is already stripped from this <code>Map</code>.
     */
    private Map<String, Object> getCurrentUser(String... attributes) {
        GetResponse getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + getEtmPrincipal().getId())
                .setFetchSource(attributes, null));
        if (getResponse.isSourceEmpty() || getResponse.getSourceAsMap().isEmpty()) {
            return new HashMap<>();
        }
        return getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, getResponse.getSourceAsMap());
    }

    /**
     * Updates the current user.
     *
     * @param source The source map without a namespace with the values to be updated.
     */
    private void updateCurrentUser(Map<String, Object> source, boolean async) {
        var objectMap = new HashMap<String, Object>();
        var builder = new UpdateRequestBuilder().setIndex(ElasticsearchLayout.CONFIGURATION_INDEX_NAME);
        builder.setId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + getEtmPrincipal().getId());
        objectMap.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, source);
        new RequestEnhancer(etmConfiguration).enhance(builder)
                .setDoc(objectMap)
                .setDocAsUpsert(true);
        if (async) {
            dataRepository.updateAsync(builder, DataRepository.noopActionListener());
        } else {
            dataRepository.update(builder);
        }
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

    private void addSearchHits(JsonBuilder builder, SearchHits hits) {
        for (SearchHit searchHit : hits.getHits()) {
            builder.startObject();
            builder.field("index", searchHit.getIndex());
            builder.field("id", searchHit.getId());
            builder.rawField("source", searchHit.getSourceAsString());
            builder.endObject();
        }
    }
}
