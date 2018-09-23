package com.jecstar.etm.gui.rest.services.signal;

import com.jecstar.etm.gui.rest.services.AbstractUserAttributeService;
import com.jecstar.etm.gui.rest.services.Keyword;
import com.jecstar.etm.gui.rest.services.dashboard.DateInterval;
import com.jecstar.etm.gui.rest.services.dashboard.MultiBucketResult;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.AggregationKey;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.AggregationValue;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.DateTimeAggregationKey;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.DoubleAggregationValue;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.signaler.SignalSearchRequestBuilderBuilder;
import com.jecstar.etm.signaler.domain.Signal;
import com.jecstar.etm.signaler.domain.converter.SignalConverter;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentileRanks;
import org.elasticsearch.search.aggregations.metrics.percentiles.Percentiles;
import org.joda.time.DateTime;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.DateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Path("/signal")
@DeclareRoles(SecurityRoles.ALL_ROLES)
public class SignalService extends AbstractUserAttributeService {


    private static Client client;
    private static EtmConfiguration etmConfiguration;
    private final EtmPrincipalConverterJsonImpl etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
    private final EtmPrincipalTags etmPrincipalTags = etmPrincipalConverter.getTags();
    private final SignalConverter signalConverter = new SignalConverter();

    public static void initialize(Client client, EtmConfiguration etmConfiguration) {
        SignalService.client = client;
        SignalService.etmConfiguration = etmConfiguration;
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
        Map<String, Object> entity = getEntity(client, groupName, this.etmPrincipalTags.getSignalDatasourcesTag());
        List<String> datasources = getArray(this.etmPrincipalTags.getSignalDatasourcesTag(), entity);
        StringBuilder result = new StringBuilder();
        result.append("{ \"keywords\":[");
        boolean first = true;
        for (String indexName : datasources) {
            Map<String, List<Keyword>> names = getIndexFields(client, indexName);
            Set<Map.Entry<String, List<Keyword>>> entries = names.entrySet();
            for (Map.Entry<String, List<Keyword>> entry : entries) {
                if (!first) {
                    result.append(", ");
                }
                first = false;
                result.append("{");
                result.append("\"index\": ").append(escapeToJson(indexName, true)).append(",");
                result.append("\"type\": ").append(escapeToJson(entry.getKey(), true)).append(",");
                result.append("\"keywords\": [").append(entry.getValue().stream().map(n -> {
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
        }
        result.append("]}");
        return result.toString();
    }

    @GET
    @Path("/datasources")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_SIGNAL_READ_WRITE)
    public String getDatasources() {
        return getSignalDatasources(null);
    }

    @GET
    @Path("/{groupName}/datasources/")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.GROUP_SIGNAL_READ, SecurityRoles.GROUP_SIGNAL_READ_WRITE})
    public Response getGroupDatasources(@PathParam("groupName") String groupName) {
        if (!getEtmPrincipal().isInGroup(groupName)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String content = getSignalDatasources(groupName);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Returns the signal datasources of a user or group.
     *
     * @param groupName The name of the group to query for signal datasources. When <code>null</code> the user signal datasources are returned.
     * @return The signal datasources of a user or group.
     */
    private String getSignalDatasources(String groupName) {
        Map<String, Object> objectMap = getEntity(client, groupName, this.etmPrincipalTags.getSignalDatasourcesTag());
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
    @RolesAllowed({SecurityRoles.GROUP_SIGNAL_READ, SecurityRoles.GROUP_SETTINGS_READ_WRITE})
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
        Map<String, Object> objectMap = getEntity(client, groupName, this.etmPrincipalTags.getSignalsTag());
        if (objectMap == null || objectMap.isEmpty()) {
            return "{\"max_signals\": " + etmConfiguration.getMaxSignalCount() + "}";
        }
        objectMap.put("max_signals", etmConfiguration.getMaxSignalCount());
        return toString(objectMap);
    }


    @PUT
    @Path("/signal/{signalName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_SIGNAL_READ_WRITE)
    public String addGraph(@PathParam("signalName") String signalName, String json) {
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
        Map<String, Object> objectMap = getEntity(client, groupName, this.etmPrincipalTags.getSignalsTag(), this.etmPrincipalTags.getSignalDatasourcesTag());
        List<String> allowedDatasources = getArray(this.etmPrincipalTags.getSignalDatasourcesTag(), objectMap);
        if (allowedDatasources.indexOf(signal.getDataSource()) < 0) {
            throw new EtmException(EtmException.NOT_AUTHORIZED_FOR_SIGNAL_DATA_SOURCE);
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
        updateEntity(client, etmConfiguration, groupName, source);
        return "{\"status\":\"success\"}";
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
        Map<String, Object> objectMap = getEntity(client, groupName, this.etmPrincipalTags.getSignalsTag());

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
        updateEntity(client, etmConfiguration, groupName, source);
        return "{\"status\":\"success\"}";
    }

    @POST
    @Path("/visualize")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.USER_SIGNAL_READ_WRITE})
    public String getGraphData(String json) {
        Signal signal = this.signalConverter.read(json);
        EtmPrincipal etmPrincipal = getEtmPrincipal();
        Map<String, Object> objectMap = getEntity(client, null, this.etmPrincipalTags.getSignalDatasourcesTag());
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
        GetResponse getResponse = client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName).get();
        if (!getResponse.isExists()) {
            return Response.noContent().build();
        }
        EtmGroup etmGroup = this.etmPrincipalConverter.readGroup(getResponse.getSourceAsMap());
        Map<String, Object> objectMap = getEntity(client, groupName, this.etmPrincipalTags.getSignalDatasourcesTag());
        List<String> allowedDatasources = getArray(this.etmPrincipalTags.getSignalDatasourcesTag(), objectMap);
        String content = getGraphData(signal, getEtmPrincipal(), etmGroup, allowedDatasources);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    private String getGraphData(Signal signal, EtmPrincipal etmPrincipal, EtmGroup etmGroup, List<String> allowedDatasources) {
        SignalSearchRequestBuilderBuilder signalSearchRequestBuilderBuilder = new SignalSearchRequestBuilderBuilder(client, etmConfiguration).setSignal(signal);
        if (allowedDatasources.indexOf(signal.getDataSource()) < 0) {
            throw new EtmException(EtmException.NOT_AUTHORIZED_FOR_DASHBOARD_DATA_SOURCE);
        }
        SearchRequestBuilder requestBuilder;
        if (etmGroup != null) {
            requestBuilder = signalSearchRequestBuilderBuilder.build(q -> addFilterQuery(etmGroup, q, etmPrincipal), etmPrincipal);
        } else {
            requestBuilder = signalSearchRequestBuilderBuilder.build(q -> addFilterQuery(etmPrincipal, q), etmPrincipal);
        }

        MultiBucketResult multiBucketResult = new MultiBucketResult();
        multiBucketResult.setAddMissingKeys(true);
        DateFormat bucketDateFormat = DateInterval.safeValueOf(signal.getCardinalityTimeunit().name()).getDateFormat(etmPrincipal.getLocale(), etmPrincipal.getTimeZone());

        // Start building the response
        StringBuilder result = new StringBuilder();
        result.append("{");
        result.append("\"d3_formatter\": ").append(getD3Formatter(getEtmPrincipal()));
        result.append(",\"data\": ");

        int exceededCount = 0;
        SearchResponse searchResponse = requestBuilder.get();
        MultiBucketsAggregation aggregation = searchResponse.getAggregations().get(SignalSearchRequestBuilderBuilder.CARDINALITY_AGGREGATION_KEY);
        for (MultiBucketsAggregation.Bucket bucket : aggregation.getBuckets()) {
            DateTime dateTime = (DateTime) bucket.getKey();
            AggregationKey key = new DateTimeAggregationKey(dateTime.getMillis(), bucketDateFormat);
            for (Aggregation subAggregation : bucket.getAggregations()) {
                AggregationValue<?> aggregationValue = getMetricAggregationValueFromAggregator(subAggregation);
                multiBucketResult.addValueToSerie(aggregationValue.getLabel(), key, aggregationValue);
                if (aggregationValue.hasValidValue() && signal.getThreshold() != null && signal.getComparison() != null) {
                    if (Signal.Comparison.LT.equals(signal.getComparison()) && aggregationValue.isLessThan(signal.getThreshold())) {
                        exceededCount++;
                    } else if (Signal.Comparison.LTE.equals(signal.getComparison()) && (aggregationValue.isLessThan(signal.getThreshold()) || aggregationValue.isEqualTo(signal.getThreshold()))) {
                        exceededCount++;
                    } else if (Signal.Comparison.EQ.equals(signal.getComparison()) && aggregationValue.isEqualTo(signal.getThreshold())) {
                        exceededCount++;
                    } else if (Signal.Comparison.GTE.equals(signal.getComparison()) && (aggregationValue.isGreaterThan(signal.getThreshold()) || aggregationValue.isEqualTo(signal.getThreshold()))) {
                        exceededCount++;
                    } else if (Signal.Comparison.GT.equals(signal.getComparison()) && aggregationValue.isGreaterThan(signal.getThreshold())) {
                        exceededCount++;
                    }
                }
            }
        }
        multiBucketResult.appendAsArrayToJsonBuffer(this, result, true);
        if (signal.getThreshold() != null && signal.getComparison() != null) {
            addIntegerElementToJsonBuffer("exceeded_count", exceededCount, result, false);
        }
        result.append("}");
        return result.toString();
    }

    private AggregationValue<?> getMetricAggregationValueFromAggregator(Aggregation aggregation) {
        if (aggregation instanceof Percentiles) {
            Percentiles percentiles = (Percentiles) aggregation;
            return new DoubleAggregationValue(aggregation.getName(), percentiles.iterator().next().getValue());
        } else if (aggregation instanceof PercentileRanks) {
            PercentileRanks percentileRanks = (PercentileRanks) aggregation;
            return new DoubleAggregationValue(aggregation.getName(), percentileRanks.iterator().next().getPercent()).setPercentage(true);
        } else if (aggregation instanceof NumericMetricsAggregation.SingleValue) {
            NumericMetricsAggregation.SingleValue singleValue = (NumericMetricsAggregation.SingleValue) aggregation;
            return new DoubleAggregationValue(aggregation.getName(), singleValue.value());
        }
        throw new IllegalArgumentException("'" + aggregation.getClass().getName() + "' is an invalid metric aggregator.");
    }

}
