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

package com.jecstar.etm.gui.rest.services.dashboard;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.gui.rest.services.AbstractUserAttributeService;
import com.jecstar.etm.gui.rest.services.dashboard.domain.Column;
import com.jecstar.etm.gui.rest.services.dashboard.domain.Dashboard;
import com.jecstar.etm.gui.rest.services.dashboard.domain.Data;
import com.jecstar.etm.gui.rest.services.dashboard.domain.GraphContainer;
import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.DashboardConverter;
import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.GraphContainerConverter;
import com.jecstar.etm.gui.rest.services.dashboard.domain.graph.*;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.BucketAggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.BucketKey;
import com.jecstar.etm.server.core.domain.aggregator.metric.MetricValue;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.EtmSecurityEntity;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalTagsJsonImpl;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.persisting.EtmQueryBuilder;
import com.jecstar.etm.server.core.persisting.RequestEnhancer;
import com.jecstar.etm.server.core.util.DateUtils;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.aggregations.ParsedMultiBucketAggregation;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation.Bucket;
import org.elasticsearch.search.aggregations.bucket.ParsedSingleBucketAggregation;
import org.elasticsearch.search.aggregations.bucket.SingleBucketAggregation;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Path("/visualization")
@DeclareRoles(SecurityRoles.ALL_ROLES)
public class DashboardService extends AbstractUserAttributeService {

    private static DataRepository dataRepository;
    private static EtmConfiguration etmConfiguration;
    private static RequestEnhancer requestEnhancer;
    private final EtmPrincipalTags principalTags = new EtmPrincipalTagsJsonImpl();
    private final GraphContainerConverter graphContainerConverter = new GraphContainerConverter();
    private final DashboardConverter dashboardConverter = new DashboardConverter();

    public static void initialize(DataRepository dataRepository, EtmConfiguration etmConfiguration) {
        DashboardService.dataRepository = dataRepository;
        DashboardService.etmConfiguration = etmConfiguration;
        DashboardService.requestEnhancer = new RequestEnhancer(etmConfiguration);
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
        var entity = getEntity(dataRepository, groupName, this.principalTags.getDashboardDatasourcesTag());
        var datasources = new HashSet<String>(getArray(this.principalTags.getDashboardDatasourcesTag(), entity, Collections.emptyList()));
        if (groupName == null) {
//          We try to get the data for the user but when the user is added to some groups the context of that groups needs to be added to the user context
            var groups = getEtmPrincipal().getGroups();
            for (var group : groups) {
                var groupObjectMap = getEntity(dataRepository, group.getName(), this.principalTags.getDashboardDatasourcesTag());
                datasources.addAll(getArray(this.principalTags.getDashboardDatasourcesTag(), groupObjectMap, Collections.emptyList()));
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
        Map<String, Object> objectMap = getEntity(dataRepository, groupName, this.principalTags.getDashboardDatasourcesTag());
        if (groupName == null) {
            // We try to get the data for the user but when the user is added to some groups the context of that groups needs to be added to the user context
            Set<EtmGroup> groups = getEtmPrincipal().getGroups();
            for (EtmGroup group : groups) {
                Map<String, Object> groupObjectMap = getEntity(dataRepository, group.getName(), this.principalTags.getDashboardDatasourcesTag());
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
        Map<String, Object> objectMap = getEntity(dataRepository, groupName, this.principalTags.getGraphsTag());
        if (objectMap == null || objectMap.isEmpty()) {
            objectMap = new HashMap<>();
        }
        objectMap.put("max_graphs", etmConfiguration.getMaxGraphCount());
        objectMap.put("timeZone", getEtmPrincipal().getTimeZone().getID());
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
        Map<String, Object> objectMap = getEntity(dataRepository, groupName, this.principalTags.getGraphsTag(), this.principalTags.getDashboardDatasourcesTag());
        EtmSecurityEntity etmSecurityEntity = getEtmSecurityEntity(dataRepository, groupName);
        GraphContainer graphContainer = this.graphContainerConverter.read(json);
        if (!etmSecurityEntity.isAuthorizedForDashboardDatasource(graphContainer.getData().getDataSource())) {
            throw new EtmException(EtmException.NOT_AUTHORIZED_FOR_DASHBOARD_DATA_SOURCE);
        }
        graphContainer.normalizeQueryTimesToInstant(getEtmPrincipal());
        createGraphSearchRequest(getEtmPrincipal(), graphContainer);

        // Data seems ok, now store the graph.
        List<Map<String, Object>> currentGraphContainers = new ArrayList<>();
        Map<String, Object> graphContainerValues = toMap(this.graphContainerConverter.write(graphContainer));
        graphContainerValues.put(GraphContainer.NAME, graphName);
        if (objectMap != null && objectMap.containsKey(this.principalTags.getGraphsTag())) {
            currentGraphContainers = getArray(this.principalTags.getGraphsTag(), objectMap);
        }
        ListIterator<Map<String, Object>> iterator = currentGraphContainers.listIterator();
        boolean updated = false;
        while (iterator.hasNext()) {
            Map<String, Object> graphContainerMap = iterator.next();
            if (graphName.equals(getString(GraphContainer.NAME, graphContainerMap))) {
                iterator.set(graphContainerValues);
                updated = true;
                break;
            }
        }
        if (!updated) {
            if (currentGraphContainers.size() >= etmConfiguration.getMaxGraphCount()) {
                throw new EtmException(EtmException.MAX_NR_OF_GRAPHS_REACHED);
            }
            currentGraphContainers.add(graphContainerValues);
        }
        Map<String, Object> source = new HashMap<>();
        source.put(this.principalTags.getGraphsTag(), currentGraphContainers);
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
        Map<String, Object> objectMap = getEntity(dataRepository, groupName, this.principalTags.getDashboardsTag(), this.principalTags.getGraphsTag());

        List<Map<String, Object>> currentGraphContainers = new ArrayList<>();
        if (objectMap == null || objectMap.isEmpty()) {
            return "{\"status\":\"success\"}";
        }
        if (objectMap.containsKey(this.principalTags.getGraphsTag())) {
            currentGraphContainers = getArray(this.principalTags.getGraphsTag(), objectMap);
        }
        ListIterator<Map<String, Object>> iterator = currentGraphContainers.listIterator();
        while (iterator.hasNext()) {
            Map<String, Object> graphContainerValues = iterator.next();
            if (graphName.equals(getString(GraphContainer.NAME, graphContainerValues))) {
                iterator.remove();
                break;
            }
        }

        Map<String, Object> source = new HashMap<>();
        // Prepare new source map with the remaining graphs.
        source.put(this.principalTags.getGraphsTag(), currentGraphContainers);

        // Now remove the graph from all dashboards.
        if (objectMap.containsKey(this.principalTags.getDashboardsTag())) {
            boolean dashboardsUpdated = false;
            List<Map<String, Object>> arrayData = getArray(this.principalTags.getDashboardsTag(), objectMap);
            if (arrayData != null) {
                List<Dashboard> dashboards = arrayData.stream().map(this.dashboardConverter::read).collect(Collectors.toList());
                for (Dashboard dashboard : dashboards) {
                    if (dashboard.removeGraph(graphName)) {
                        dashboardsUpdated = true;
                    }
                }
                if (dashboardsUpdated) {
                    source.put(this.principalTags.getDashboardsTag(), dashboards.stream().map(d -> toMap(this.dashboardConverter.write(d))).collect(Collectors.toList()));
                }
            }
        }
        updateEntity(dataRepository, etmConfiguration, groupName, source);
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
        Map<String, Object> objectMap = getEntity(dataRepository, groupName, this.principalTags.getDashboardsTag());
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
            dashboardNames.add(getString(Dashboard.NAME, dashboardData));
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
        Map<String, Object> objectMap = getEntity(dataRepository, groupName, this.principalTags.getDashboardsTag());
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
            if (dashboardName.equals(getString(Dashboard.NAME, dashboardData))) {
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
        Dashboard dashboard = this.dashboardConverter.read(json);
        dashboard.setName(dashboardName);
        // Data seems ok, now store the graph.
        Map<String, Object> objectMap = getEntity(dataRepository, groupName, this.principalTags.getDashboardsTag());

        List<Map<String, Object>> currentDashboards = new ArrayList<>();
        if (objectMap != null && objectMap.containsKey(this.principalTags.getDashboardsTag())) {
            currentDashboards = getArray(this.principalTags.getDashboardsTag(), objectMap);
        }
        ListIterator<Map<String, Object>> dashboardIterator = currentDashboards.listIterator();
        boolean updated = false;
        while (dashboardIterator.hasNext()) {
            Map<String, Object> db = dashboardIterator.next();
            if (dashboardName.equals(getString(Dashboard.NAME, db))) {
                dashboardIterator.set(toMap(this.dashboardConverter.write(dashboard)));
                updated = true;
                break;
            }
        }
        if (!updated) {
            if (currentDashboards.size() >= etmConfiguration.getMaxGraphCount()) {
                throw new EtmException(EtmException.MAX_NR_OF_DASHBOARDS_REACHED);
            }
            currentDashboards.add(toMap(this.dashboardConverter.write(dashboard)));
        }
        Map<String, Object> source = new HashMap<>();
        source.put(this.principalTags.getDashboardsTag(), currentDashboards);
        updateEntity(dataRepository, etmConfiguration, groupName, source);
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
        Map<String, Object> objectMap = getEntity(dataRepository, groupName, this.principalTags.getDashboardsTag());
        List<Map<String, Object>> currentDashboards = new ArrayList<>();
        if (objectMap != null && objectMap.containsKey(this.principalTags.getDashboardsTag())) {
            currentDashboards = getArray(this.principalTags.getDashboardsTag(), objectMap);
        }
        ListIterator<Map<String, Object>> iterator = currentDashboards.listIterator();
        while (iterator.hasNext()) {
            Map<String, Object> dashboard = iterator.next();
            if (dashboardName.equals(getString(Dashboard.NAME, dashboard))) {
                iterator.remove();
                break;
            }
        }
        Map<String, Object> source = new HashMap<>();
        source.put(this.principalTags.getDashboardsTag(), currentDashboards);
        updateEntity(dataRepository, etmConfiguration, groupName, source);
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
        return getGraphData(this.graphContainerConverter.read(json), getEtmPrincipal());
    }

    @POST
    @Path("/{groupName}/graphdata")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.USER_DASHBOARD_READ_WRITE, SecurityRoles.GROUP_DASHBOARD_READ_WRITE})
    public String getGroupPreviewGraphData(@PathParam("groupName") String groupName, String json) {
        return getGraphData(this.graphContainerConverter.read(json), getEtmGroup(dataRepository, groupName));
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
        Map<String, Object> objectMap = getEntity(dataRepository, groupName, this.principalTags.getDashboardsTag(), this.principalTags.getGraphsTag(), this.principalTags.getDashboardDatasourcesTag());
        EtmSecurityEntity etmSecurityEntity = getEtmSecurityEntity(dataRepository, groupName);
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
        List<Dashboard> dashboards = dashboardsData.stream().map(this.dashboardConverter::read).collect(Collectors.toList());
        List<GraphContainer> graphContainers = graphsData.stream().map(this.graphContainerConverter::read).collect(Collectors.toList());

        Column column = null;
        for (Dashboard dashboard : dashboards) {
            if (!dashboardName.equals(dashboard.getName())) {
                continue;
            }
            column = dashboard.getColumnById(graphId);
            break;
        }
        if (column == null || column.getGraphName() == null) {
            return null;
        }
        // now find the graph and return the data.
        for (GraphContainer graphContainer : graphContainers) {
            if (column.getGraphName().equals(graphContainer.getName())) {
                graphContainer.mergeFromColumn(column);
                return getGraphData(graphContainer, etmSecurityEntity);
            }
        }
        return null;
    }

    private String getGraphData(GraphContainer graphContainer, EtmSecurityEntity etmSecurityEntity) {
        if (!etmSecurityEntity.isAuthorizedForDashboardDatasource(graphContainer.getData().getDataSource())) {
            throw new EtmException(EtmException.NOT_AUTHORIZED_FOR_DASHBOARD_DATA_SOURCE);
        }
        if (NumberGraph.TYPE.equals(graphContainer.getGraph().getType())) {
            return getSingleValueData(graphContainer);
        } else if (BarGraph.TYPE.equals(graphContainer.getGraph().getType())) {
            return getMultiBucketData(graphContainer);
        } else if (LineGraph.TYPE.equals(graphContainer.getGraph().getType())) {
            return getMultiBucketData(graphContainer);
        } else if (AreaGraph.TYPE.equals(graphContainer.getGraph().getType())) {
            return getMultiBucketData(graphContainer);
        } else if (PieGraph.TYPE.equals(graphContainer.getGraph().getType())) {
            return getMultiBucketData(graphContainer);
        } else if (ScatterGraph.TYPE.equals(graphContainer.getGraph().getType())) {
            return getMultiBucketData(graphContainer);
        } else {
            throw new RuntimeException("Unknown graph type: '" + graphContainer.getGraph().getType() + "'.");
        }
    }

    private String getSingleValueData(GraphContainer graphContainer) {
        var etmPrincipal = getEtmPrincipal();
        var searchResponse = dataRepository.search(createGraphSearchRequest(etmPrincipal, graphContainer));
        Aggregation aggregation = null;
        for (Aggregation aggregationUnderInvestigation : searchResponse.getAggregations().asList()) {
            var showOnGraph = (boolean) aggregationUnderInvestigation.getMetaData().get(Aggregator.SHOW_ON_GRAPH);
            if (showOnGraph) {
                aggregation = aggregationUnderInvestigation;
                break;
            }
        }
        final var metricValue = extractSingleLeafMetricValue(aggregation);
        var builder = initializeGraphResult(etmPrincipal, graphContainer);
        if (metricValue != null) {
            builder.field("value", metricValue.getJsonValue());
        }
        builder.startObject("chart_config");
        builder.startObject("credits").field("enabled", false).endObject();
        graphContainer.getGraph().appendHighchartsConfig(builder);
        builder.endObject().endObject();
        return builder.build();
    }

    /**
     * Extracts a <code>MetricValue</code> from a tree of <code>Aggregation</code>s. If the tree does contain multibucket aggregators, or multiple aggregators in a single bucket aggregator <code>null</code> will be returned.
     *
     * @param aggregation The <code>Aggregation</code> to extract the leaf <code>MetricValue</code> from.
     * @return The <code>MetricValue</code> or <code>null</code> if no or multiple <code>MetricValue</code>s could be extracted.
     */
    private MetricValue extractSingleLeafMetricValue(Aggregation aggregation) {
        if (aggregation instanceof SingleBucketAggregation) {
            List<Aggregation> aggregations = ((SingleBucketAggregation) aggregation).getAggregations().asList();
            if (aggregations.size() == 1) {
                return extractSingleLeafMetricValue(aggregations.get(0));
            }
            return null;
        }
        return new MetricValue(aggregation);
    }

    private String getMultiBucketData(GraphContainer graphContainer) {
        var etmPrincipal = getEtmPrincipal();
        var searchResponse = dataRepository.search(createGraphSearchRequest(etmPrincipal, graphContainer));
        var aggregation = searchResponse.getAggregations().asList().get(0);

        // Start building the response
        var builder = initializeGraphResult(etmPrincipal, graphContainer);
        builder.startObject("chart_config");
        builder.startObject("credits").field("enabled", false).endObject();
        builder.startObject("tooltip").field("shared", true).endObject();
        builder.startObject("time").field("timezone", etmPrincipal.getTimeZone().toZoneId().toString()).endObject();
        graphContainer.getGraph().appendHighchartsConfig(builder);
        if (!(aggregation instanceof MultiBucketsAggregation)) {
            throw new IllegalArgumentException("'" + aggregation.getClass().getName() + "' is an invalid multi bucket aggregation.");
        }
        Map<String, List<String>> seriesData = new LinkedHashMap<>();
        MultiBucketsAggregation multiBucketsAggregation = (MultiBucketsAggregation) aggregation;
        final boolean bucketKeyAsString = (boolean) multiBucketsAggregation.getMetaData().get(BucketAggregator.METADATA_BUCKET_KEY_AS_STRING);
        for (Bucket bucket : multiBucketsAggregation.getBuckets()) {
            final BucketKey bucketKey = new BucketKey(bucket, bucketKeyAsString);
            if (bucket.getAggregations().asList().size() == 0) {
                String bucketName = multiBucketsAggregation.getMetaData().get(Aggregator.NAME) + "(" + bucket.getKey() + ")";
                final MetricValue metricValue = new MetricValue(bucketName, bucket.getDocCount());
                addToSeries(seriesData, bucketKey, metricValue, bucketName);
            } else {
                processAggregations(bucketKey, bucket, seriesData, "");
            }
        }
        builder.startArray("series");
        for (var entry : seriesData.entrySet()) {
            builder.startObject();
            builder.field("name", entry.getKey());
            builder.startArray("data");
            for (var value : entry.getValue()) {
                builder.rawElement(value);
            }
            builder.endArray();
            builder.endObject();
        }
        builder.endArray().endObject().endObject();
        return builder.build();
    }

    private JsonBuilder initializeGraphResult(EtmPrincipal etmPrincipal, GraphContainer graphContainer) {
        var builder = new JsonBuilder();
        builder.startObject();
        builder.field("locale", getLocalFormatting(etmPrincipal));
        builder.field("type", graphContainer.getGraph().getType());
        builder.field("valueFormat", graphContainer.getGraph().getValueFormat());
        return builder;
    }

    /**
     * Process all aggregations for a multi aggregationHolder graph.
     *
     * @param root              The root aggregationHolder.
     * @param aggregationHolder The aggregationHolder to process.
     * @param seriesData        The series data.
     * @param currentName       The current name of the hierarchy.
     */
    private void processAggregations(BucketKey root, HasAggregations aggregationHolder, Map<String, List<String>> seriesData, String currentName) {
        for (Aggregation aggregation : aggregationHolder.getAggregations()) {
            String name = currentName;
            boolean showOnGraph = (boolean) aggregation.getMetaData().get(Aggregator.SHOW_ON_GRAPH);
            if (!showOnGraph) {
                continue;
            }
            if (aggregation instanceof ParsedMultiBucketAggregation) {
                ParsedMultiBucketAggregation multiBucketsAggregation = (ParsedMultiBucketAggregation) aggregation;
                String bucketName = (String) multiBucketsAggregation.getMetaData().get(Aggregator.NAME);
                name = createHierarchicalBucketName(name, bucketName);
                for (Bucket subBucket : multiBucketsAggregation.getBuckets()) {
                    final String subBucketName = name + "(" + subBucket.getKeyAsString() + ")";
                    if (subBucket.getAggregations().asList().size() == 0) {
                        final MetricValue metricValue = new MetricValue(subBucketName, subBucket.getDocCount());
                        addToSeries(seriesData, root, metricValue, subBucketName);
                    } else {
                        processAggregations(root, subBucket, seriesData, subBucketName);
                    }
                }
            } else if (aggregation instanceof ParsedSingleBucketAggregation) {
                ParsedSingleBucketAggregation singleBucketAggregation = (ParsedSingleBucketAggregation) aggregation;
                String bucketName = (String) singleBucketAggregation.getMetaData().get(Aggregator.NAME);
                name = createHierarchicalBucketName(name, bucketName);
                processAggregations(root, singleBucketAggregation, seriesData, name);
            } else {
                final MetricValue metricValue = new MetricValue(aggregation);
                if (name.length() == 0) {
                    name = metricValue.getName();
                } else {
                    name += ": " + metricValue.getName();
                }
                addToSeries(seriesData, root, metricValue, name);
            }
        }
    }

    private String createHierarchicalBucketName(String currentName, String bucketName) {
        if (currentName.length() != 0) {
            currentName += " > ";
        }
        return currentName + bucketName;
    }

    private void addToSeries(Map<String, List<String>> seriesData, BucketKey bucketKey, MetricValue metricValue, String serieName) {
        List<String> values = seriesData.computeIfAbsent(serieName, k -> new ArrayList<>());
        if (metricValue.hasValidValue()) {
            // If we want to allow gaps in the graph we have to remove this if statement.
            values.add("[" + bucketKey.getJsonValue() + ", " + metricValue.getJsonValue() + "]");
        } else {
            values.add("[" + bucketKey.getJsonValue() + ", " + 0 + "]");
        }
    }

    private SearchRequestBuilder createGraphSearchRequest(EtmPrincipal etmPrincipal, GraphContainer graphContainer) {
        Data data = graphContainer.getData();
        SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder()).setIndices(etmConfiguration.mergeRemoteIndices(data.getDataSource()))
                .setFetchSource(false)
                .setSize(0);
        var etmQueryBuilder = new EtmQueryBuilder(data.getQuery(), null, dataRepository, requestEnhancer).setTimeZone(etmPrincipal.getTimeZone().getID());

        if (data.getFrom() != null || data.getTill() != null) {
            RangeQueryBuilder timestampFilter = new RangeQueryBuilder(data.getTimeFilterField() != null ? data.getTimeFilterField() : "timestamp");
            boolean needsUtcZone = false;
            if (data.getFrom() != null) {
                Instant instant = DateUtils.parseDateString(data.getFrom(), etmPrincipal.getTimeZone().toZoneId(), true);
                if (instant != null) {
                    timestampFilter.gte("" + instant.toEpochMilli());
                    needsUtcZone = true;
                } else {
                    timestampFilter.gte(data.getFrom());
                }
            }
            if (data.getTill() != null) {
                Instant instant = DateUtils.parseDateString(data.getTill(), etmPrincipal.getTimeZone().toZoneId(), false);
                if (instant != null) {
                    timestampFilter.lte("" + instant.toEpochMilli());
                    needsUtcZone = true;
                } else {
                    timestampFilter.lte(data.getTill());
                }
            }
            if (!needsUtcZone) {
                timestampFilter.timeZone(etmPrincipal.getTimeZone().getID());
            }
            etmQueryBuilder.filterRoot(timestampFilter).filterJoin(timestampFilter);
        }

        if (ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL.equals(data.getDataSource())) {
            searchRequestBuilder.setQuery(addFilterQuery(getEtmPrincipal(), etmQueryBuilder.buildRootQuery()));
        } else {
            searchRequestBuilder.setQuery(etmQueryBuilder.buildRootQuery());
        }
        graphContainer.getGraph().prepareForSearch(dataRepository, searchRequestBuilder);
        graphContainer.getGraph().addAggregators(searchRequestBuilder);
        return searchRequestBuilder;
    }
}
