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

package com.jecstar.etm.gui.rest.services.signal;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.gui.rest.services.AbstractUserAttributeService;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.BucketAggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.BucketKey;
import com.jecstar.etm.server.core.domain.aggregator.metric.MetricValue;
import com.jecstar.etm.server.core.domain.cluster.notifier.Notifier;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.EtmSecurityEntity;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.persisting.RequestEnhancer;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import com.jecstar.etm.signaler.SignalSearchRequestBuilderBuilder;
import com.jecstar.etm.signaler.domain.Signal;
import com.jecstar.etm.signaler.domain.Threshold;
import com.jecstar.etm.signaler.domain.converter.SignalConverter;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.aggregations.ParsedMultiBucketAggregation;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.ParsedSingleBucketAggregation;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/signal")
@DeclareRoles(SecurityRoles.ALL_ROLES)
public class SignalService extends AbstractUserAttributeService {


    private static DataRepository dataRepository;
    private static EtmConfiguration etmConfiguration;
    private static RequestEnhancer requestEnhancer;
    private final EtmPrincipalConverterJsonImpl etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
    private final EtmPrincipalTags etmPrincipalTags = etmPrincipalConverter.getTags();
    private final SignalConverter signalConverter = new SignalConverter();

    public static void initialize(DataRepository dataRepository, EtmConfiguration etmConfiguration) {
        SignalService.dataRepository = dataRepository;
        SignalService.etmConfiguration = etmConfiguration;
        SignalService.requestEnhancer = new RequestEnhancer(etmConfiguration);
    }

    @GET
    @Path("/keywords")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.USER_SIGNAL_READ_WRITE})
    public String getKeywords() {
        return getKeywords(null);
    }

    @GET
    @Path("/{groupName}/keywords")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.GROUP_SIGNAL_READ, SecurityRoles.GROUP_SIGNAL_READ_WRITE})
    public Response getGroupKeywords(@PathParam("groupName") String groupName) {
        if (!getEtmPrincipal().isInGroup(groupName)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String content = getKeywords(groupName);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Gives the keywords of all indices the user of group has authorizations for.
     *
     * @param groupName The name of the group to retrieve the keywords for. When <code>null</code> the user keywords are returned.
     * @return The keywords of a user or group.
     */
    private String getKeywords(String groupName) {
        var entity = getEntity(dataRepository, groupName, this.etmPrincipalTags.getSignalDatasourcesTag());
        var datasources = new HashSet<String>(getArray(this.etmPrincipalTags.getSignalDatasourcesTag(), entity, Collections.emptyList()));
        if (groupName == null) {
//          We try to get the data for the user but when the user is added to some groups the context of that groups needs to be added to the user context
            var groups = getEtmPrincipal().getGroups();
            for (var group : groups) {
                var groupObjectMap = getEntity(dataRepository, group.getName(), this.etmPrincipalTags.getSignalDatasourcesTag());
                datasources.addAll(getArray(this.etmPrincipalTags.getSignalDatasourcesTag(), groupObjectMap, Collections.emptyList()));
            }
        }
        var builder = new JsonBuilder();
        builder.startObject();
        builder.startArray("keywords");
        for (String indexName : datasources) {
            var keywords = getIndexFields(dataRepository, indexName);
            builder.startObject();
            builder.field("index", indexName);
            builder.startArray("keywords");
            for (var keyword : keywords) {
                builder.startObject();
                builder.field("name", keyword.getName());
                builder.field("type", keyword.getType());
                builder.field("date", keyword.isDate());
                builder.field("number", keyword.isNumber());
                builder.endObject();
            }
            builder.endArray().endObject();
        }
        builder.endArray().endObject();
        return builder.build();
    }

    @GET
    @Path("/contextdata")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_SIGNAL_READ_WRITE)
    public String getContextData() {
        return getContextData(null);
    }

    @GET
    @Path("/{groupName}/contextdata/")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.GROUP_SIGNAL_READ, SecurityRoles.GROUP_SIGNAL_READ_WRITE})
    public Response getGroupContextData(@PathParam("groupName") String groupName) {
        if (!getEtmPrincipal().isInGroup(groupName)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String content = getContextData(groupName);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Returns the signal context data of a user or group.
     *
     * @param groupName The name of the group to query for signal context data. When <code>null</code> the user signal context data is returned.
     * @return The signal context data of a user or group.
     */
    private String getContextData(String groupName) {
        Map<String, Object> objectMap = getEntity(dataRepository, groupName, this.etmPrincipalTags.getSignalDatasourcesTag(), this.etmPrincipalTags.getNotifiersTag());
        List<String> notifiers = getArray(this.etmPrincipalTags.getNotifiersTag(), objectMap, new ArrayList<>());
        if (groupName == null) {
            // We try to get the data for the user but when the user is added to some groups the context of that groups needs to be added to the user context
            Set<EtmGroup> groups = getEtmPrincipal().getGroups();
            for (EtmGroup group : groups) {
                Map<String, Object> groupObjectMap = getEntity(dataRepository, group.getName(), this.etmPrincipalTags.getSignalDatasourcesTag(), this.etmPrincipalTags.getNotifiersTag());
                mergeCollectionInValueMap(groupObjectMap, objectMap, this.etmPrincipalTags.getSignalDatasourcesTag());
                List<String> groupNotifiers = getArray(this.etmPrincipalTags.getNotifiersTag(), groupObjectMap, new ArrayList<>());
                for (String notifier : groupNotifiers) {
                    if (!notifiers.contains(notifier)) {
                        notifiers.add(notifier);
                    }
                }
            }
        }
        objectMap.remove(this.etmPrincipalTags.getNotifiersTag());
        if (notifiers != null) {
            ArrayList<Map<String, Object>> notifiersList = new ArrayList<>();
            objectMap.put(this.etmPrincipalTags.getNotifiersTag(), notifiersList);


            SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder())
                    .setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                    .setFetchSource(new String[]{
                            ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER + "." + Notifier.NAME,
                            ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER + "." + Notifier.NOTIFIER_TYPE
                    }, null)
                    .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER));
            ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
            for (var searchHit : scrollableSearch) {
                Map<String, Object> notifierMap = toMapWithoutNamespace(searchHit.getSourceAsMap(), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER);
                String notifierName = getString(Notifier.NAME, notifierMap);
                if (notifiers.contains(notifierName)) {
                    notifiersList.add(notifierMap);
                }
            }
        }
        // Load all notifiers because we need to give the notifier type to the signal page.
        return toString(objectMap);
    }

    @GET
    @Path("/signals")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_SIGNAL_READ_WRITE)
    public String getSignals() {
        return getSignals(null);
    }

    @GET
    @Path("/{groupName}/signals/")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.GROUP_SIGNAL_READ, SecurityRoles.GROUP_SIGNAL_READ_WRITE})
    public Response getGroupSignals(@PathParam("groupName") String groupName) {
        if (!getEtmPrincipal().isInGroup(groupName)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String content = getSignals(groupName);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Returns the signals of a user or group.
     *
     * @param groupName The name of the group to query for signals. When <code>null</code> the user signals are returned.
     * @return The signals of a user or group.
     */
    private String getSignals(String groupName) {
        Map<String, Object> objectMap = getEntity(dataRepository, groupName, this.etmPrincipalTags.getSignalsTag());
        if (objectMap == null || objectMap.isEmpty()) {
            objectMap = new HashMap<>();
        }
        objectMap.put("max_signals", etmConfiguration.getMaxSignalCount());
        objectMap.put("timeZone", getEtmPrincipal().getTimeZone().getID());
        return toString(objectMap);
    }


    @PUT
    @Path("/signal/{signalName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_SIGNAL_READ_WRITE)
    public String addSignal(@PathParam("signalName") String signalName, String json) {
        return addSignal(null, signalName, json);
    }

    @PUT
    @Path("/{groupName}/signal/{signalName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.GROUP_SIGNAL_READ_WRITE)
    public Response addGroupSignal(@PathParam("groupName") String groupName, @PathParam("signalName") String signalName, String json) {
        if (!getEtmPrincipal().isInGroup(groupName)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String content = addSignal(groupName, signalName, json);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Add a signal to an entity.
     *
     * @param groupName  The name of the group to add the signal to. When <code>null</code> the signal will be stored on the user.
     * @param signalName The name of the signal.
     * @param json       The signal data.
     * @return The json result of the addition.
     */
    private String addSignal(String groupName, String signalName, String json) {
        Signal signal = this.signalConverter.read(json);
        signal.normalizeQueryTimesToInstant(getEtmPrincipal());
        EtmSecurityEntity etmSecurityEntity = getEtmSecurityEntity(dataRepository, groupName);
        Map<String, Object> objectMap = getEntity(dataRepository, groupName, this.etmPrincipalTags.getSignalsTag(), this.etmPrincipalTags.getSignalDatasourcesTag(), this.etmPrincipalTags.getNotifiersTag());
        if (!etmSecurityEntity.isAuthorizedForSignalDatasource(signal.getData().getDataSource())) {
            throw new EtmException(EtmException.NOT_AUTHORIZED_FOR_SIGNAL_DATA_SOURCE);
        }
        for (String notifier : signal.getNotifications().getNotifiers()) {
            if (!etmSecurityEntity.isAuthorizedForNotifier(notifier)) {
                throw new EtmException(EtmException.NOT_AUTHORIZED_FOR_NOTIFIER);
            }
        }
        List<Map<String, Object>> currentSignals = new ArrayList<>();
        Map<String, Object> signalData = toMap(this.signalConverter.write(signal));
        if (objectMap != null && objectMap.containsKey(this.etmPrincipalTags.getSignalsTag())) {
            currentSignals = getArray(this.etmPrincipalTags.getSignalsTag(), objectMap);
        }
        ListIterator<Map<String, Object>> iterator = currentSignals.listIterator();
        boolean updated = false;
        while (iterator.hasNext()) {
            Map<String, Object> signalMap = iterator.next();
            if (signalName.equals(getString("name", signalMap))) {
                // Copy metadata that isn't store by the converter.
                for (String key : Signal.METADATA_KEYS) {
                    if (signalMap.containsKey(key)) {
                        signalData.put(key, signalMap.get(key));
                    }
                }
                iterator.set(signalData);
                updated = true;
                break;
            }
        }
        if (!updated) {
            if (currentSignals.size() >= etmConfiguration.getMaxSignalCount()) {
                throw new EtmException(EtmException.MAX_NR_OF_SIGNALS_REACHED);
            }
            currentSignals.add(signalData);
        }
        Map<String, Object> source = new HashMap<>();
        source.put(this.etmPrincipalTags.getSignalsTag(), currentSignals);
        updateEntity(dataRepository, etmConfiguration, groupName, source);
        return "{\"status\":\"success\"}";
    }

    private EtmSecurityEntity getEtmSecurityEntity(DataRepository dataRepository, String groupName) {
        if (groupName != null) {
            return getEtmGroup(dataRepository, groupName);
        }
        return getEtmPrincipal();
    }

    @DELETE
    @Path("/signal/{signalName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_SIGNAL_READ_WRITE)
    public String deleteSignal(@PathParam("signalName") String signalName) {
        return deleteSignal(null, signalName);
    }

    @DELETE
    @Path("/{groupName}/signal/{signalName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.GROUP_SIGNAL_READ_WRITE)
    public Response deleteGroupSignal(@PathParam("groupName") String groupName, @PathParam("signalName") String signalName) {
        if (!getEtmPrincipal().isInGroup(groupName)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String content = deleteSignal(groupName, signalName);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Deletes a signal.
     *
     * @param groupName  The name of the group to delete the graph from. When <code>null</code> the graph will be deleted from the user.
     * @param signalName The name of the signal to delete.
     * @return The json result of the delete.
     */
    private String deleteSignal(String groupName, String signalName) {
        Map<String, Object> objectMap = getEntity(dataRepository, groupName, this.etmPrincipalTags.getSignalsTag());

        List<Map<String, Object>> currentSignals = new ArrayList<>();
        if (objectMap == null || objectMap.isEmpty()) {
            return "{\"status\":\"success\"}";
        }
        if (objectMap.containsKey(this.etmPrincipalTags.getSignalsTag())) {
            currentSignals = getArray(this.etmPrincipalTags.getSignalsTag(), objectMap);
        }
        ListIterator<Map<String, Object>> iterator = currentSignals.listIterator();
        while (iterator.hasNext()) {
            Map<String, Object> signalData = iterator.next();
            if (signalName.equals(getString("name", signalData))) {
                iterator.remove();
                break;
            }
        }

        // Prepare new source map with the remaining signals.
        Map<String, Object> source = new HashMap<>();
        source.put(this.etmPrincipalTags.getSignalsTag(), currentSignals);
        updateEntity(dataRepository, etmConfiguration, groupName, source);
        return "{\"status\":\"success\"}";
    }

    @POST
    @Path("/visualize")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.USER_SIGNAL_READ_WRITE})
    public String getGraphData(String json) {
        Signal signal = this.signalConverter.read(json);
        EtmPrincipal etmPrincipal = getEtmPrincipal();
        Map<String, Object> objectMap = getEntity(dataRepository, null, this.etmPrincipalTags.getSignalDatasourcesTag());
        List<String> allowedDatasources = getArray(this.etmPrincipalTags.getSignalDatasourcesTag(), objectMap);
        return getGraphData(signal, etmPrincipal, null, allowedDatasources);
    }

    @POST
    @Path("/{groupName}/visualize")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.GROUP_SIGNAL_READ_WRITE})
    public Response getGroupGraphData(@PathParam("groupName") String groupName, String json) {
        Signal signal = this.signalConverter.read(json);
        if (!getEtmPrincipal().isInGroup(groupName)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        GetResponse getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName));
        if (!getResponse.isExists()) {
            return Response.noContent().build();
        }
        EtmGroup etmGroup = this.etmPrincipalConverter.readGroup(getResponse.getSourceAsMap());
        Map<String, Object> objectMap = getEntity(dataRepository, groupName, this.etmPrincipalTags.getSignalDatasourcesTag());
        List<String> allowedDatasources = getArray(this.etmPrincipalTags.getSignalDatasourcesTag(), objectMap);
        String content = getGraphData(signal, getEtmPrincipal(), etmGroup, allowedDatasources);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    private String getGraphData(Signal signal, EtmPrincipal etmPrincipal, EtmGroup etmGroup, List<String> allowedDatasources) {
        var signalSearchRequestBuilderBuilder = new SignalSearchRequestBuilderBuilder(dataRepository, etmConfiguration).setSignal(signal);
        if (allowedDatasources.indexOf(signal.getData().getDataSource()) < 0) {
            throw new EtmException(EtmException.NOT_AUTHORIZED_FOR_DASHBOARD_DATA_SOURCE);
        }
        SearchRequestBuilder requestBuilder;
        if (etmGroup != null) {
            requestBuilder = signalSearchRequestBuilderBuilder.build(q -> addFilterQuery(etmGroup, q, etmPrincipal), etmPrincipal);
        } else {
            requestBuilder = signalSearchRequestBuilderBuilder.build(q -> addFilterQuery(etmPrincipal, q), etmPrincipal);
        }
        // Start building the response
        var builder = new JsonBuilder();
        builder.startObject();
        builder.rawField("locale", getLocalFormatting(getEtmPrincipal()));
        int exceededCount = 0;
        SearchResponse searchResponse = dataRepository.search(requestBuilder);
        Map<String, List<String>> seriesData = new LinkedHashMap<>();
        MultiBucketsAggregation multiBucketsAggregation = searchResponse.getAggregations().get(SignalSearchRequestBuilderBuilder.CARDINALITY_AGGREGATION_KEY);
        final boolean bucketKeyAsString = (boolean) multiBucketsAggregation.getMetaData().get(BucketAggregator.METADATA_BUCKET_KEY_AS_STRING);
        for (MultiBucketsAggregation.Bucket bucket : multiBucketsAggregation.getBuckets()) {
            final BucketKey bucketKey = new BucketKey(bucket, bucketKeyAsString);
            if (bucket.getAggregations().asList().size() == 0) {
                String bucketName = multiBucketsAggregation.getMetaData().get(Aggregator.NAME) + "(" + bucket.getKey() + ")";
                final MetricValue metricValue = new MetricValue(bucketName, bucket.getDocCount());
                addToSeries(seriesData, bucketKey, metricValue, bucketName);
                if (metricValue.hasValidValue() && signal.getThreshold().getComparison().isExceeded(signal.getThreshold().getValue(), metricValue.getValue())) {
                    exceededCount++;
                }
            } else {
                exceededCount = processAggregations(bucketKey, bucket, seriesData, "", signal.getThreshold(), exceededCount);
            }
        }
        if (signal.getThreshold() != null && signal.getThreshold().getComparison() != null) {
            builder.field("exceeded_count", exceededCount);
        }
        builder.startObject("chart_config");
        builder.startObject("credits").field("enabled", false).endObject();
        builder.startObject("tooltip").field("shared", true).endObject();
        builder.startObject("title").field("text", "Signal visualization").endObject();
        builder.startObject("time").field("timezone", etmPrincipal.getTimeZone().toZoneId().toString()).endObject();
        builder.startObject("xAxis").field("type", "datetime").endObject();
        builder.startObject("yAxis").startArray("plotLines").startObject().field("value", signal.getThreshold().getValue()).field("color", "red").field("dashStyle", "shortdash").field("width", 2).startObject("label").field("text", "Threshold limit").endObject().endObject().endArray().endObject();
        builder.startObject("chart").field("type", "spline").endObject();

        builder.startObject("plotOptions").startObject("spline").startObject("marker").field("enabled", true).endObject().endObject().endObject();
        builder.startArray("series");
        for (Map.Entry<String, List<String>> entry : seriesData.entrySet()) {
            builder.startObject();
            builder.field("name", entry.getKey());
            builder.startArray("data");
            for (var value : entry.getValue()) {
                builder.rawElement(value);
            }
            builder.endArray();
            builder.startArray("zones");
            if (Threshold.Comparison.GT.equals(signal.getThreshold().getComparison()) || Threshold.Comparison.GTE.equals(signal.getThreshold().getComparison())) {
                builder.startObject().field("value", (Threshold.Comparison.GTE.equals(signal.getThreshold().getComparison()) ? signal.getThreshold().getValue() : signal.getThreshold().getValue() + 0.00000001)).endObject();
                builder.startObject().field("color", "red").endObject();
            } else if (Threshold.Comparison.LT.equals(signal.getThreshold().getComparison()) || Threshold.Comparison.LTE.equals(signal.getThreshold().getComparison())) {
                builder.startObject().field("value", (Threshold.Comparison.GTE.equals(signal.getThreshold().getComparison()) ? signal.getThreshold().getValue() : signal.getThreshold().getValue() + 0.00000001)).field("color", "red").endObject();
            } else {
                builder.startObject().field("value", (signal.getThreshold().getValue() - 0.00000001)).endObject();
                builder.startObject().field("value", (signal.getThreshold().getValue() + 0.00000001)).field("color", "red").endObject();
            }
            builder.endArray().endObject();
        }
        builder.endArray().endObject().endObject();
        return builder.build();
    }

    private int processAggregations(BucketKey root, HasAggregations aggregationHolder, Map<String, List<String>> seriesData, String currentName, Threshold threshold, int exceededCount) {
        Iterator<Aggregation> iterator = aggregationHolder.getAggregations().iterator();
        while (iterator.hasNext()) {
            String name = currentName;
            Aggregation aggregation = iterator.next();
            boolean showOnGraph = (boolean) aggregation.getMetaData().get(Aggregator.SHOW_ON_GRAPH);
            if (!showOnGraph) {
                continue;
            }
            if (aggregation instanceof ParsedMultiBucketAggregation) {
                ParsedMultiBucketAggregation multiBucketsAggregation = (ParsedMultiBucketAggregation) aggregation;
                String bucketName = (String) multiBucketsAggregation.getMetaData().get(Aggregator.NAME);
                name = createHierarchicalBucketName(name, bucketName);
                for (MultiBucketsAggregation.Bucket subBucket : multiBucketsAggregation.getBuckets()) {
                    final String subBucketName = name + "(" + subBucket.getKeyAsString() + ")";
                    if (subBucket.getAggregations().asList().size() == 0) {
                        final MetricValue metricValue = new MetricValue(subBucketName, subBucket.getDocCount());
                        addToSeries(seriesData, root, metricValue, subBucketName);
                        if (metricValue.hasValidValue() && threshold.getComparison().isExceeded(threshold.getValue(), metricValue.getValue())) {
                            exceededCount++;
                        }
                    } else {
                        exceededCount = processAggregations(root, subBucket, seriesData, name + "(" + subBucket.getKeyAsString() + ")", threshold, exceededCount);
                    }
                }
            } else if (aggregation instanceof ParsedSingleBucketAggregation) {
                ParsedSingleBucketAggregation singleBucketAggregation = (ParsedSingleBucketAggregation) aggregation;
                String bucketName = (String) singleBucketAggregation.getMetaData().get(Aggregator.NAME);
                name = createHierarchicalBucketName(name, bucketName);
                exceededCount = processAggregations(root, singleBucketAggregation, seriesData, name, threshold, exceededCount);
            } else {
                final MetricValue metricValue = new MetricValue(aggregation);
                if (name.length() == 0) {
                    name = metricValue.getName();
                } else {
                    name += ": " + metricValue.getName();
                }
                addToSeries(seriesData, root, metricValue, name);
                if (metricValue.hasValidValue() && threshold.getComparison().isExceeded(threshold.getValue(), metricValue.getValue())) {
                    exceededCount++;
                }
            }
        }
        return exceededCount;
    }

    private String createHierarchicalBucketName(String currentName, String bucketName) {
        if (currentName.length() != 0) {
            currentName += " > ";
        }
        return currentName + bucketName;
    }

    private void addToSeries(Map<String, List<String>> serieData, BucketKey bucketKey, MetricValue metricValue, String seriesName) {
        List<String> values = serieData.get(seriesName);
        if (values == null) {
            values = new ArrayList<>();
            serieData.put(seriesName, values);
        }
        if (metricValue.hasValidValue()) {
            // If we want to allow gaps in the graph we have to remove this if statement.
            values.add("[" + bucketKey.getJsonValue() + ", " + metricValue.getJsonValue() + "]");
        } else {
            values.add("[" + bucketKey.getJsonValue() + ", " + 0 + "]");
        }
    }
}
