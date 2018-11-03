package com.jecstar.etm.gui.rest.services.dashboard;

import com.jecstar.etm.gui.rest.services.AbstractUserAttributeService;
import com.jecstar.etm.gui.rest.services.Keyword;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.*;
import com.jecstar.etm.gui.rest.services.dashboard.domain.*;
import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.GraphConverter;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.EtmSecurityEntity;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalTagsJsonImpl;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation.Bucket;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation.SingleValue;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentileRanks;
import org.elasticsearch.search.aggregations.metrics.percentiles.Percentiles;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.Format;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Path("/visualization")
@DeclareRoles(SecurityRoles.ALL_ROLES)
public class DashboardService extends AbstractUserAttributeService {

    private static Client client;
    private static EtmConfiguration etmConfiguration;
    private final EtmPrincipalTags principalTags = new EtmPrincipalTagsJsonImpl();
    private final GraphConverter graphConverter = new GraphConverter();

    public static void initialize(Client client, EtmConfiguration etmConfiguration) {
        DashboardService.client = client;
        DashboardService.etmConfiguration = etmConfiguration;
    }

    @GET
    @Path("/keywords")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.USER_DASHBOARD_READ_WRITE})
    public String getKeywords() {
        return getKeywords(null);
    }

    @GET
    @Path("/{groupName}/keywords")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.GROUP_DASHBOARD_READ, SecurityRoles.GROUP_DASHBOARD_READ_WRITE})
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
        Map<String, Object> entity = getEntity(client, groupName, this.principalTags.getDashboardDatasourcesTag());
        List<String> datasources = getArray(this.principalTags.getDashboardDatasourcesTag(), entity);
        StringBuilder result = new StringBuilder();
        result.append("{ \"keywords\":[");
        boolean first = true;
        for (String indexName : datasources) {
            Map<String, List<Keyword>> names = getIndexFields(client, indexName);
            Set<Entry<String, List<Keyword>>> entries = names.entrySet();
            for (Entry<String, List<Keyword>> entry : entries) {
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
    @RolesAllowed(SecurityRoles.USER_DASHBOARD_READ_WRITE)
    public String getDatasources() {
        return getDashboardDatasources(null);
    }

    @GET
    @Path("/{groupName}/datasources/")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.GROUP_DASHBOARD_READ, SecurityRoles.GROUP_DASHBOARD_READ_WRITE})
    public Response getGroupDatasources(@PathParam("groupName") String groupName) {
        if (!getEtmPrincipal().isInGroup(groupName)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String content = getDashboardDatasources(groupName);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Returns the dashboard datasources of a user or group.
     *
     * @param groupName The name of the group to query for dashboard datasources. When <code>null</code> the user dashboard datasources are returned.
     * @return The dashboard datasources of a user or group.
     */
    private String getDashboardDatasources(String groupName) {
        Map<String, Object> objectMap = getEntity(client, groupName, this.principalTags.getDashboardDatasourcesTag());
        if (groupName == null) {
            // We try to get the data for the user but when the user is added to some groups the context of that groups needs to be added to the user context
            Set<EtmGroup> groups = getEtmPrincipal().getGroups();
            for (EtmGroup group : groups) {
                Map<String, Object> groupObjectMap = getEntity(client, group.getName(), this.principalTags.getDashboardDatasourcesTag());
                mergeCollectionInValueMap(groupObjectMap, objectMap, this.principalTags.getDashboardDatasourcesTag());
            }
        }
        return toString(objectMap);
    }

    @GET
    @Path("/graphs")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_DASHBOARD_READ_WRITE)
    public String getGraphs() {
        return getGraphs(null);
    }

    @GET
    @Path("/{groupName}/graphs/")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.GROUP_DASHBOARD_READ, SecurityRoles.GROUP_DASHBOARD_READ_WRITE})
    public Response getGroupGraphs(@PathParam("groupName") String groupName) {
        if (!getEtmPrincipal().isInGroup(groupName)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String content = getGraphs(groupName);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Returns the graphs of a user or group.
     *
     * @param groupName The name of the group to query for graphs. When <code>null</code> the user graphs are returned.
     * @return The graphs of a user or group.
     */
    private String getGraphs(String groupName) {
        Map<String, Object> objectMap = getEntity(client, groupName, this.principalTags.getGraphsTag());
        if (objectMap == null || objectMap.isEmpty()) {
            return "{\"max_graphs\": " + etmConfiguration.getMaxGraphCount() + "}";
        }
        objectMap.put("max_graphs", etmConfiguration.getMaxGraphCount());
        return toString(objectMap);
    }

    @PUT
    @Path("/graph/{graphName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_DASHBOARD_READ_WRITE)
    public String addGraph(@PathParam("graphName") String graphName, String json) {
        return addGraph(null, graphName, json);
    }

    @PUT
    @Path("/{groupName}/graph/{graphName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.GROUP_DASHBOARD_READ_WRITE)
    public Response addGroupGraph(@PathParam("groupName") String groupName, @PathParam("graphName") String graphName, String json) {
        if (!getEtmPrincipal().isInGroup(groupName)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String content = addGraph(groupName, graphName, json);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Add a graph to an entity.
     *
     * @param groupName The name of the group to add the graph to. When <code>null</code> the graph will be stored on the user.
     * @param graphName The name of the graph.
     * @param json      The graph data.
     * @return The json result of the addition.
     */
    private String addGraph(String groupName, String graphName, String json) {
        // Execute a dry run on all data.
        Map<String, Object> objectMap = getEntity(client, groupName, this.principalTags.getGraphsTag(), this.principalTags.getDashboardDatasourcesTag());
        EtmSecurityEntity etmSecurityEntity = getEtmSecurityEntity(client, groupName);

        Graph graph = this.graphConverter.read(json);

        getGraphData(graph, etmSecurityEntity, true);
        // Data seems ok, now store the graph.
        List<Map<String, Object>> currentGraphs = new ArrayList<>();
        Map<String, Object> graphData = toMap(this.graphConverter.write(graph));
        graphData.put("name", graphName);
        if (objectMap != null && objectMap.containsKey(this.principalTags.getGraphsTag())) {
            currentGraphs = getArray(this.principalTags.getGraphsTag(), objectMap);
        }
        ListIterator<Map<String, Object>> iterator = currentGraphs.listIterator();
        boolean updated = false;
        while (iterator.hasNext()) {
            Map<String, Object> graphMap = iterator.next();
            if (graphName.equals(getString("name", graphMap))) {
                iterator.set(graphData);
                updated = true;
                break;
            }
        }
        if (!updated) {
            if (currentGraphs.size() >= etmConfiguration.getMaxGraphCount()) {
                throw new EtmException(EtmException.MAX_NR_OF_GRAPHS_REACHED);
            }
            currentGraphs.add(graphData);
        }
        Map<String, Object> source = new HashMap<>();
        source.put(this.principalTags.getGraphsTag(), currentGraphs);
        updateEntity(client, etmConfiguration, groupName, source);
        return "{\"status\":\"success\"}";
    }

    private EtmSecurityEntity getEtmSecurityEntity(Client client, String groupName) {
        if (groupName != null) {
            return getEtmGroup(client, groupName);
        }
        return getEtmPrincipal();
    }

    @DELETE
    @Path("/graph/{graphName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_DASHBOARD_READ_WRITE)
    public String deleteGraph(@PathParam("graphName") String graphName) {
        return deleteGraph(null, graphName);
    }

    @DELETE
    @Path("/{groupName}/graph/{graphName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.GROUP_DASHBOARD_READ_WRITE)
    public Response deleteGroupGraph(@PathParam("groupName") String groupName, @PathParam("graphName") String graphName) {
        if (!getEtmPrincipal().isInGroup(groupName)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String content = deleteGraph(groupName, graphName);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Deletes a graph.
     *
     * @param groupName The name of the group to delete the graph from. When <code>null</code> the graph will be deleted from the user.
     * @param graphName The name of the graph to delete.
     * @return The json result of the delete.
     */
    private String deleteGraph(String groupName, String graphName) {
        Map<String, Object> objectMap = getEntity(client, groupName, this.principalTags.getDashboardsTag(), this.principalTags.getGraphsTag());

        List<Map<String, Object>> currentGraphs = new ArrayList<>();
        if (objectMap == null || objectMap.isEmpty()) {
            return "{\"status\":\"success\"}";
        }
        if (objectMap.containsKey(this.principalTags.getGraphsTag())) {
            currentGraphs = getArray(this.principalTags.getGraphsTag(), objectMap);
        }
        ListIterator<Map<String, Object>> iterator = currentGraphs.listIterator();
        while (iterator.hasNext()) {
            Map<String, Object> graphData = iterator.next();
            if (graphName.equals(getString("name", graphData))) {
                iterator.remove();
                break;
            }
        }

        Map<String, Object> source = new HashMap<>();
        // Prepare new source map with the remaining graphs.
        source.put(this.principalTags.getGraphsTag(), currentGraphs);

        // Now remove the graph from all dashboards.
        if (objectMap.containsKey(this.principalTags.getDashboardsTag())) {
            boolean dashboardsUpdated = false;
            List<Map<String, Object>> dashboardsData = getArray(this.principalTags.getDashboardsTag(), objectMap);
            if (dashboardsData != null) {
                for (Map<String, Object> dashboardData : dashboardsData) {
                    List<Map<String, Object>> rowsValues = getArray("rows", dashboardData);
                    if (rowsValues != null) {
                        for (Map<String, Object> rowValues : rowsValues) {
                            List<Map<String, Object>> colsValues = getArray("cols", rowValues);
                            if (colsValues != null) {
                                for (Map<String, Object> colValues : colsValues) {
                                    if (graphName.equals(colValues.get("name"))) {
                                        Object id = colValues.get("id");
                                        Object parts = colValues.get("parts");
                                        Object bordered = colValues.get("bordered");
                                        colValues.clear();
                                        colValues.put("id", id);
                                        colValues.put("parts", parts);
                                        colValues.put("bordered", bordered);
                                        dashboardsUpdated = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (dashboardsUpdated) {
                source.put(this.principalTags.getDashboardsTag(), dashboardsData);
            }
        }
        updateEntity(client, etmConfiguration, groupName, source);
        return "{\"status\":\"success\"}";
    }

    @GET
    @Path("/dashboards")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_DASHBOARD_READ_WRITE)
    public String getDashboards() {
        return getDashboards(null);
    }

    @GET
    @Path("/{groupName}/dashboards")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.GROUP_DASHBOARD_READ_WRITE, SecurityRoles.GROUP_DASHBOARD_READ})
    public Response getGroupDashboards(@PathParam("groupName") String groupName) {
        if (!getEtmPrincipal().isInGroup(groupName)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String content = getDashboards(groupName);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Returns the dashboard names of a user or group.
     *
     * @param groupName The name of the group to query for dashboards. When <code>null</code> the user dashboards are returned.
     * @return The dashboards of a user or group.
     */
    private String getDashboards(String groupName) {
        Map<String, Object> objectMap = getEntity(client, groupName, this.principalTags.getDashboardsTag());
        if (objectMap == null || objectMap.isEmpty()) {
            return null;
        }
        if (!objectMap.containsKey(this.principalTags.getDashboardsTag())) {
            return null;
        }
        List<Map<String, Object>> dashboardsData = getArray(this.principalTags.getDashboardsTag(), objectMap);
        if (dashboardsData == null) {
            return null;
        }
        Set<String> dashboardNames = new HashSet<>();
        for (Map<String, Object> dashboardData : dashboardsData) {
            dashboardNames.add(getString("name", dashboardData));
        }
        Map<String, Object> response = new HashMap<>();
        response.put(this.principalTags.getDashboardsTag(), dashboardNames);
        return toString(response);
    }

    @GET
    @Path("/dashboard/{dashboardName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_DASHBOARD_READ_WRITE)
    public String getDashboard(@PathParam("dashboardName") String dashboardName) {
        return getDashboard(null, dashboardName);
    }

    @GET
    @Path("/{groupName}/dashboard/{dashboardName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.GROUP_DASHBOARD_READ_WRITE, SecurityRoles.GROUP_DASHBOARD_READ})
    public Response getGroupDashboard(@PathParam("groupName") String groupName, @PathParam("dashboardName") String dashboardName) {
        if (!getEtmPrincipal().isInGroup(groupName)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String content = getDashboard(groupName, dashboardName);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Returns a dashboard of a user or group.
     *
     * @param groupName     The name of the group to query for dashboards. When <code>null</code> the user dashboard is returned.
     * @param dashboardName The name of the dashboard to return.
     * @return The dashboard of a user or group.
     */
    private String getDashboard(String groupName, String dashboardName) {
        Map<String, Object> objectMap = getEntity(client, groupName, this.principalTags.getDashboardsTag());
        if (objectMap == null || objectMap.isEmpty()) {
            return null;
        }
        if (!objectMap.containsKey(this.principalTags.getDashboardsTag())) {
            return null;
        }
        List<Map<String, Object>> dashboardsData = getArray(this.principalTags.getDashboardsTag(), objectMap);
        if (dashboardsData == null) {
            return null;
        }
        for (Map<String, Object> dashboardData : dashboardsData) {
            if (dashboardName.equals(getString("name", dashboardData))) {
                return toString(dashboardData);
            }
        }
        return null;
    }

    @PUT
    @Path("/dashboard/{dashboardName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_DASHBOARD_READ_WRITE)
    public String addDashboard(@PathParam("dashboardName") String dashboardName, String json) {
        return addDashboard(null, dashboardName, json);
    }

    @PUT
    @Path("/{groupName}/dashboard/{dashboardName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.GROUP_DASHBOARD_READ_WRITE)
    public Response addGroupDashboard(@PathParam("groupName") String groupName, @PathParam("dashboardName") String dashboardName, String json) {
        if (!getEtmPrincipal().isInGroup(groupName)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String content = addDashboard(groupName, dashboardName, json);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Add a dashboard to an entity.
     *
     * @param groupName     The name of the group to add the dashboard to. When <code>null</code> the dashboard will be stored on the user.
     * @param dashboardName The name of the dashboard.
     * @param json          The dashboard data.
     * @return The json result of the addition.
     */
    private String addDashboard(String groupName, String dashboardName, String json) {
        // Execute a dry run on all data.
        // TODO validate all data
//		getGraphData(json, true);
        // Data seems ok, now store the graph.
        Map<String, Object> objectMap = getEntity(client, groupName, this.principalTags.getDashboardsTag());

        List<Map<String, Object>> currentDashboards = new ArrayList<>();
        Map<String, Object> valueMap = toMap(json);
        valueMap.put("name", dashboardName);
        if (objectMap != null && objectMap.containsKey(this.principalTags.getDashboardsTag())) {
            currentDashboards = getArray(this.principalTags.getDashboardsTag(), objectMap);
        }
        ListIterator<Map<String, Object>> iterator = currentDashboards.listIterator();
        boolean updated = false;
        while (iterator.hasNext()) {
            Map<String, Object> dashboard = iterator.next();
            if (dashboardName.equals(getString("name", dashboard))) {
                iterator.set(valueMap);
                updated = true;
                break;
            }
        }
        if (!updated) {
            if (currentDashboards.size() >= etmConfiguration.getMaxGraphCount()) {
                throw new EtmException(EtmException.MAX_NR_OF_DASHBOARDS_REACHED);
            }
            currentDashboards.add(valueMap);
        }
        Map<String, Object> source = new HashMap<>();
        source.put(this.principalTags.getDashboardsTag(), currentDashboards);
        updateEntity(client, etmConfiguration, groupName, source);
        return "{\"status\":\"success\"}";
    }

    @DELETE
    @Path("/dashboard/{dashboardName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_DASHBOARD_READ_WRITE)
    public String deleteDashboard(@PathParam("dashboardName") String dashboardName) {
        return deleteDashboard(null, dashboardName);
    }

    @DELETE
    @Path("/{groupName}/dashboard/{dashboardName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.GROUP_DASHBOARD_READ_WRITE)
    public Response deleteGroupDashboard(@PathParam("groupName") String groupName, @PathParam("dashboardName") String dashboardName) {
        if (!getEtmPrincipal().isInGroup(groupName)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String content = deleteDashboard(groupName, dashboardName);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Deletes a dashboard.
     *
     * @param groupName     The name of the group to delete the dashboard from. When <code>null</code> the dashboard will be deleted from the user.
     * @param dashboardName The name of the dashboard to delete.
     * @return The json result of the delete.
     */
    private String deleteDashboard(String groupName, String dashboardName) {
        Map<String, Object> objectMap = getEntity(client, groupName, this.principalTags.getDashboardsTag());
        List<Map<String, Object>> currentDashboards = new ArrayList<>();
        if (objectMap != null && objectMap.containsKey(this.principalTags.getDashboardsTag())) {
            currentDashboards = getArray(this.principalTags.getDashboardsTag(), objectMap);
        }
        ListIterator<Map<String, Object>> iterator = currentDashboards.listIterator();
        while (iterator.hasNext()) {
            Map<String, Object> dashboard = iterator.next();
            if (dashboardName.equals(getString("name", dashboard))) {
                iterator.remove();
                break;
            }
        }
        Map<String, Object> source = new HashMap<>();
        source.put(this.principalTags.getDashboardsTag(), currentDashboards);
        updateEntity(client, etmConfiguration, groupName, source);
        return "{\"status\":\"success\"}";
    }

    @GET
    @Path("/graphdata/{dashboardName}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.USER_DASHBOARD_READ_WRITE)
    public String getGraphData(@PathParam("dashboardName") String dashboardName, @PathParam("id") String graphId) {
        return getGraphData(null, dashboardName, graphId);
    }

    @GET
    @Path("/{groupName}/graphdata/{dashboardName}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.GROUP_DASHBOARD_READ, SecurityRoles.GROUP_DASHBOARD_READ_WRITE})
    public Response getGroupGraphData(@PathParam("groupName") String groupName, @PathParam("dashboardName") String dashboardName, @PathParam("id") String graphId) {
        if (!getEtmPrincipal().isInGroup(groupName)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String content = getGraphData(groupName, dashboardName, graphId);
        if (content == null || content.trim().length() == 0) {
            return Response.noContent().build();
        }
        return Response.ok(content, MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/graphdata")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.USER_DASHBOARD_READ_WRITE, SecurityRoles.GROUP_DASHBOARD_READ_WRITE})
    public String getPreviewGraphData(String json) {
        return getGraphData(this.graphConverter.read(json), getEtmPrincipal(), false);
    }

    @POST
    @Path("/{groupName}/graphdata")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.USER_DASHBOARD_READ_WRITE, SecurityRoles.GROUP_DASHBOARD_READ_WRITE})
    public String getGroupPreviewGraphData(@PathParam("groupName") String groupName, String json) {
        return getGraphData(this.graphConverter.read(json), getEtmGroup(client, groupName), false);
    }

    /**
     * Returns the live data of a graph.
     *
     * @param groupName     The name of the group to that holds the dashboard. When <code>null</code> the dashboard will be retrieved from the user.
     * @param dashboardName The name of the dashboard to get the data from.
     * @param graphId       The id of the graph.
     * @return The live graph data.
     */
    private String getGraphData(String groupName, String dashboardName, String graphId) {
        Map<String, Object> objectMap = getEntity(client, groupName, this.principalTags.getDashboardsTag(), this.principalTags.getGraphsTag(), this.principalTags.getDashboardDatasourcesTag());
        EtmSecurityEntity etmSecurityEntity = getEtmSecurityEntity(client, groupName);
        if (objectMap == null || !objectMap.containsKey(this.principalTags.getDashboardsTag())) {
            return null;
        }
        if (!objectMap.containsKey(this.principalTags.getGraphsTag())) {
            return null;
        }
        List<Map<String, Object>> graphsData = getArray(this.principalTags.getGraphsTag(), objectMap);
        if (graphsData == null) {
            return null;
        }
        // First find the dashboard
        List<Map<String, Object>> dashboardsData = getArray(this.principalTags.getDashboardsTag(), objectMap);
        if (dashboardsData == null) {
            return null;
        }
        String dashboardFrom = null;
        String dashboardTill = null;
        String dashboardQuery = null;
        String dashboardTimeFilterField = null;
        String graphName = null;
        for (Map<String, Object> dashboardData : dashboardsData) {
            if (!dashboardName.equals(getString("name", dashboardData))) {
                continue;
            }
            List<Map<String, Object>> rowsValues = getArray("rows", dashboardData);
            if (rowsValues != null) {
                for (Map<String, Object> row : rowsValues) {
                    List<Map<String, Object>> colsValues = getArray("cols", row);
                    if (colsValues != null) {
                        for (Map<String, Object> col : colsValues) {
                            if (graphId.equals(getString(Graph.ID, col))) {
                                dashboardFrom = getString(Graph.FROM, col);
                                dashboardTill = getString(Graph.TILL, col);
                                dashboardTimeFilterField = getString(Graph.TIME_FILTER_FIELD, col);
                                dashboardQuery = getString(Graph.QUERY, col);
                                graphName = getString("name", col);
                            }
                        }
                    }
                }
            }
        }
        if (graphName == null || dashboardQuery == null) {
            return null;
        }
        // now find the graph and return the data.
        for (Map<String, Object> graphData : graphsData) {
            if (graphName.equals(getString("name", graphData))) {
                graphData.put(Graph.FROM, dashboardFrom);
                graphData.put(Graph.TILL, dashboardTill);
                graphData.put(Graph.TIME_FILTER_FIELD, dashboardTimeFilterField);
                graphData.put(Graph.QUERY, dashboardQuery);
                return getGraphData(this.graphConverter.read(graphData), etmSecurityEntity, false);
            }
        }
        return null;
    }

    private String getGraphData(Graph graph, EtmSecurityEntity etmSecurityEntity, boolean dryRun) {
        if (!etmSecurityEntity.isAuthorizedForDashboardDatasource(graph.getDataSource())) {
            throw new EtmException(EtmException.NOT_AUTHORIZED_FOR_DASHBOARD_DATA_SOURCE);
        }
        if (NumberGraph.TYPE.equals(graph.getType())) {
            return getNumberData((NumberGraph) graph, dryRun);
        } else if (BarGraph.TYPE.equals(graph.getType())) {
            return getMultiBucketData((MultiBucketGraph) graph, dryRun);
        } else if (LineGraph.TYPE.equals(graph.getType())) {
            return getMultiBucketData((MultiBucketGraph) graph, dryRun);
        } else if (StackedAreaGraph.TYPE.equals(graph.getType())) {
            return getMultiBucketData((MultiBucketGraph) graph, dryRun);
        } else {
            throw new RuntimeException("Unknown type: '" + graph.getType() + "'.");
        }
    }

    private String getMultiBucketData(MultiBucketGraph graph, boolean dryRun) {
        EtmPrincipal etmPrincipal = getEtmPrincipal();
        MultiBucketResult multiBucketResult = new MultiBucketResult(graph);
        SearchRequestBuilder searchRequest = createGraphSearchRequest(etmPrincipal, graph);
        Axes axes = graph.getAxes();

        BucketAggregatorWrapper bucketAggregatorWrapper = new BucketAggregatorWrapper(etmPrincipal, axes.getXAxis().getBucketAggregator(), createGraphSearchRequest(etmPrincipal, graph));
        BucketAggregatorWrapper bucketSubAggregatorWrapper = null;

        if (bucketAggregatorWrapper.needsMetricSubAggregatorForSorting() && axes.getXAxis().getBucketSubAggregator() != null) {
            // We are in the situation that all metric aggregators will be put on the bucketSubAggregator but the (root)
            // bucketAggregator needs a metric aggregator for sorting. We add the requested metric aggregator on the
            // bucketAggregator and will strip it from the results as soon as the json result json is created.
            bucketAggregatorWrapper.setSortOverrideAggregator(axes.getYAxis().getMetricAggregators());
        }

        // First create the bucket aggregator
        AggregationBuilder bucketAggregatorBuilder = bucketAggregatorWrapper.getAggregationBuilder();
        AggregationBuilder rootForMetricAggregators = bucketAggregatorBuilder;
        // Check for the presence of a sub aggregator
        if (axes.getXAxis().getBucketSubAggregator() != null) {
            bucketSubAggregatorWrapper = new BucketAggregatorWrapper(etmPrincipal, axes.getXAxis().getBucketSubAggregator());
            rootForMetricAggregators = bucketSubAggregatorWrapper.getAggregationBuilder();
            bucketAggregatorBuilder.subAggregation(rootForMetricAggregators);
        }

        // And add every y-axis aggregator as sub aggregator
        for (MetricAggregator metricAggregator : axes.getYAxis().getMetricAggregators()) {
            rootForMetricAggregators.subAggregation(new MetricAggregatorWrapper(etmPrincipal, metricAggregator).getAggregationBuilder());
        }
        if (dryRun) {
            return null;
        }
        SearchResponse searchResponse = searchRequest.addAggregation(bucketAggregatorBuilder).get();
        Aggregation aggregation = searchResponse.getAggregations().get(bucketAggregatorBuilder.getName());

        // Start building the response
        StringBuilder result = new StringBuilder();
        result.append("{");
        result.append("\"locale\": ").append(getLocalFormatting(etmPrincipal));
        addStringElementToJsonBuffer("type", graph.getType(), result, false);
        result.append(",\"data\": ");

        if (!(aggregation instanceof MultiBucketsAggregation)) {
            throw new IllegalArgumentException("'" + aggregation.getClass().getName() + "' is an invalid multi bucket aggregation.");
        }
        MultiBucketsAggregation multiBucketsAggregation = (MultiBucketsAggregation) aggregation;
        for (Bucket bucket : multiBucketsAggregation.getBuckets()) {
// When we provide the option to remove empty results from the list this if statement should be enabled.
//            if (bucket.getDocCount() == 0) {
//                continue;
//            }
            AggregationKey key = getFormattedBucketKey(bucket, bucketAggregatorWrapper.getBucketFormat());
            for (Aggregation subAggregation : bucket.getAggregations()) {
                if (BucketAggregatorWrapper.SORT_METRIC_ID.equals(subAggregation.getName())) {
                    continue;
                }
                if (subAggregation instanceof MultiBucketsAggregation) {
                    // A sub aggregation on the x-axis.
                    MultiBucketsAggregation subBucketsAggregation = (MultiBucketsAggregation) subAggregation;
                    for (Bucket subBucket : subBucketsAggregation.getBuckets()) {
// When we provide the option to remove empty results from the list this if statement should be enabled.
//                        if (subBucket.getDocCount() == 0) {
//                            continue;
//                        }
                        AggregationKey subKey = getFormattedBucketKey(subBucket, bucketSubAggregatorWrapper.getBucketFormat());
                        for (Aggregation metricAggregation : subBucket.getAggregations()) {
                            AggregationValue<?> aggregationValue = getMetricAggregationValueFromAggregator(metricAggregation);
                            multiBucketResult.addValueToSerie(subKey.getKeyAsString() + ": " + aggregationValue.getLabel(), key, aggregationValue);
                        }
                    }
                } else {
                    AggregationValue<?> aggregationValue = getMetricAggregationValueFromAggregator(subAggregation);
                    multiBucketResult.addValueToSerie(aggregationValue.getLabel(), key, aggregationValue);
                }
            }
        }
        multiBucketResult.appendAsArrayToJsonBuffer(this, result, true);
        result.append("}");
        return result.toString();
    }

    private AggregationKey getFormattedBucketKey(Bucket bucket, Format format) {
        if (bucket.getKey() instanceof DateTime) {
            DateTime dateTime = (DateTime) bucket.getKey();
            return new DateTimeAggregationKey(Instant.ofEpochMilli(dateTime.getMillis()), format);
        } else if (bucket.getKey() instanceof Double) {
            return new DoubleAggregationKey((Double) bucket.getKey(), format);
        } else if (bucket.getKey() instanceof Long) {
            return new LongAggregationKey((Long) bucket.getKey(), format);
        }
        return new StringAggregationKey(bucket.getKeyAsString());
    }

    private String getNumberData(NumberGraph graph, boolean dryRun) {
        EtmPrincipal etmPrincipal = getEtmPrincipal();
        SearchRequestBuilder searchRequest = createGraphSearchRequest(
                etmPrincipal,
                graph);


        MetricAggregatorWrapper metricAggregatorWrapper = new MetricAggregatorWrapper(etmPrincipal, graph.getMetricAggregator());
        if (dryRun) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        result.append("{");
        result.append("\"locale\": ").append(getLocalFormatting(etmPrincipal));
        addStringElementToJsonBuffer("type", "number", result, false);
        result.append(", \"data\": {");
        addStringElementToJsonBuffer("aggregator", metricAggregatorWrapper.getAggregatorType(), result, true);

        AggregationBuilder aggregatorBuilder = metricAggregatorWrapper.getAggregationBuilder();
        SearchResponse searchResponse = searchRequest.addAggregation(aggregatorBuilder).get();
        Aggregation aggregation = searchResponse.getAggregations().get(aggregatorBuilder.getName());
        AggregationValue<?> aggregationValue = getMetricAggregationValueFromAggregator(aggregation);
        aggregationValue.appendToJsonBuffer(this, metricAggregatorWrapper.getFieldFormat(), result, false);

        result.append("}}");
        return result.toString();
    }

    private SearchRequestBuilder createGraphSearchRequest(EtmPrincipal etmPrincipal, Graph graph) {
        SearchRequestBuilder searchRequest = enhanceRequest(client.prepareSearch(graph.getDataSource()), etmConfiguration)
                .setFetchSource(false)
                .setSize(0);
        QueryStringQueryBuilder queryStringBuilder = new QueryStringQueryBuilder(graph.getQuery())
                .allowLeadingWildcard(true)
                .analyzeWildcard(true)
                .timeZone(DateTimeZone.forTimeZone(etmPrincipal.getTimeZone()));
        queryStringBuilder.defaultField(ElasticsearchLayout.ETM_ALL_FIELDS_ATTRIBUTE_NAME);

        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(queryStringBuilder);
        if (graph.getFrom() != null || graph.getTill() != null) {
            RangeQueryBuilder timestampFilter = new RangeQueryBuilder(graph.getTimeFilterField() != null ? graph.getTimeFilterField() : "timestamp");
            if (graph.getFrom() != null) {
                timestampFilter.gte(graph.getFrom());
            }
            if (graph.getTill() != null) {
                timestampFilter.lte(graph.getTill());
            }
            boolQueryBuilder.filter(timestampFilter);
        }


        if (ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL.equals(graph.getDataSource())) {
            searchRequest.setQuery(addFilterQuery(getEtmPrincipal(), boolQueryBuilder));
        } else {
            searchRequest.setQuery(boolQueryBuilder);
        }
        return searchRequest;
    }

    private AggregationValue<?> getMetricAggregationValueFromAggregator(Aggregation aggregation) {
        Map<String, Object> metadata = aggregation.getMetaData();
        if (aggregation instanceof Percentiles) {
            Percentiles percentiles = (Percentiles) aggregation;
            return new DoubleAggregationValue(metadata.get("label").toString(), percentiles.iterator().next().getValue());
        } else if (aggregation instanceof PercentileRanks) {
            PercentileRanks percentileRanks = (PercentileRanks) aggregation;
            return new DoubleAggregationValue(metadata.get("label").toString(), percentileRanks.iterator().next().getPercent()).setPercentage(true);
        } else if (aggregation instanceof NumericMetricsAggregation.SingleValue) {
            NumericMetricsAggregation.SingleValue singleValue = (SingleValue) aggregation;
            return new DoubleAggregationValue(metadata.get("label").toString(), singleValue.value());
        }
        throw new IllegalArgumentException("'" + aggregation.getClass().getName() + "' is an invalid metric aggregator.");
    }

}
