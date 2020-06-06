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

package com.jecstar.etm.gui.rest.services.iib;

import com.ibm.broker.config.proxy.ConfigManagerProxyLoggedException;
import com.ibm.broker.config.proxy.ConfigurableService;
import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.gui.rest.AbstractGuiService;
import com.jecstar.etm.gui.rest.IIBApi;
import com.jecstar.etm.gui.rest.services.iib.proxy.*;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.audit.ConfigurationChangedAuditLog;
import com.jecstar.etm.server.core.domain.audit.builder.ConfigurationChangedAuditLogBuilder;
import com.jecstar.etm.server.core.domain.audit.converter.ConfigurationChangedAuditLogConverter;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.*;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.RequestEnhancer;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;

@Path("/iib")
@DeclareRoles(SecurityRoles.ALL_ROLES)
public class IIBService extends AbstractGuiService {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(IIBService.class);

    private static final String MONITORING_PROFILES = "MonitoringProfiles";
    private static final String PROFILE_PROPERTIES = "profileProperties";

    private static DataRepository dataRepository;
    private static RequestEnhancer requestEnhancer;

    private final NodeConverter nodeConverter = new NodeConverter();
    private final ConfigurationChangedAuditLogConverter configurationChangedAuditLogConverter = new ConfigurationChangedAuditLogConverter();

    public static void initialize(DataRepository dataRepository, EtmConfiguration etmConfiguration) {
        IIBService.dataRepository = dataRepository;
        IIBService.requestEnhancer = new RequestEnhancer(etmConfiguration);
    }

    @GET
    @Path("/nodes")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.IIB_NODE_READ, SecurityRoles.IIB_NODE_READ_WRITE})
    public String getNodes() {
        SearchRequestBuilder searchRequestBuilder = requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
                .setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE));
        ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
        if (!scrollableSearch.hasNext()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        result.append("{\"nodes\": [");
        boolean first = true;
        for (var searchHit : scrollableSearch) {
            if (!first) {
                result.append(",");
            }
            result.append(toStringWithoutNamespace(searchHit.getSourceAsMap(), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE));
            first = false;
        }
        result.append("]}");
        return result.toString();
    }

    @DELETE
    @Path("/node/{nodeName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.IIB_NODE_READ_WRITE)
    public String deleteNode(@PathParam("nodeName") String nodeName) {
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldNodeConfiguration = getCurrentNodeConfiguration(nodeName);

        DeleteRequestBuilder builder = requestEnhancer.enhance(
                new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX + nodeName)
        );
        dataRepository.delete(builder);
        if (oldNodeConfiguration != null) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(ConfigurationChangedAuditLog.Action.DELETE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX + nodeName)
                    .setOldValue(oldNodeConfiguration)
            );
        }
        return "{\"status\":\"success\"}";
    }

    @PUT
    @Path("/node/{nodeName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.IIB_NODE_READ_WRITE)
    public String addNode(@PathParam("nodeName") String nodeName, String json) {
        var now = Instant.now();
        var etmPrincipal = getEtmPrincipal();
        var oldNodeConfiguration = getCurrentNodeConfiguration(nodeName);

        Node node = this.nodeConverter.read(toStringWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE));
        try (IIBNodeConnection nodeConnection = createIIBConnectionInstance(node)) {
            nodeConnection.connect();
        }
        var newNodeConfiguration = this.nodeConverter.write(node);
        UpdateRequestBuilder builder = requestEnhancer.enhance(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX + nodeName)
        )
                .setDoc(newNodeConfiguration, XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true);
        dataRepository.update(builder);
        if (!Objects.equals(oldNodeConfiguration, newNodeConfiguration)) {
            storeConfigurationChangedAuditLog(new ConfigurationChangedAuditLogBuilder()
                    .setPrincipalId(etmPrincipal.getId())
                    .setHandlingTime(now)
                    .setAction(oldNodeConfiguration == null ? ConfigurationChangedAuditLog.Action.CREATE : ConfigurationChangedAuditLog.Action.UPDATE)
                    .setConfigurationType(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE)
                    .setConfigurationId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX + nodeName)
                    .setOldValue(oldNodeConfiguration)
                    .setNewValue(newNodeConfiguration)
            );
        }
        return "{ \"status\": \"success\" }";
    }

    /**
     * Returns the current node configuration.
     *
     * @return The current node configuration.
     */
    private String getCurrentNodeConfiguration(String nodeName) {
        var getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX + nodeName)
                .setFetchSource(true)
        );
        if (!getResponse.isExists()) {
            return null;
        }
        var nodeConfig = this.nodeConverter.read(getResponse.getSourceAsString());
        return this.nodeConverter.write(nodeConfig);
    }

    @GET
    @Path("/node/{nodeName}/servers")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.IIB_EVENT_READ, SecurityRoles.IIB_EVENT_READ_WRITE})
    public String getServers(@PathParam("nodeName") String nodeName) {
        var getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX + nodeName)
                .setFetchSource(true));
        if (!getResponse.isExists()) {
            return null;
        }
        var node = this.nodeConverter.read(getResponse.getSourceAsString());
        var builder = new JsonBuilder();
        builder.startObject();
        var servers = new ArrayList<String>();
        try (IIBNodeConnection nodeConnection = createIIBConnectionInstance(node)) {
            nodeConnection.connect();
            List<IIBIntegrationServer> integrationServers = nodeConnection.getServers();
            for (IIBIntegrationServer integrationServer : integrationServers) {
                servers.add(integrationServer.getName());
            }
        }
        builder.field("servers", servers);
        builder.endObject();
        return builder.build();
    }

    @POST
    @Path("/node/{nodeName}/server/{serverName}/{objectType}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.IIB_EVENT_READ_WRITE)
    public String updateEventMonitoring(@PathParam("nodeName") String nodeName, @PathParam("serverName") String serverName, @PathParam("objectType") String objectType, String json) {
        Map<String, Object> valueMap = toMap(json);
        GetResponse getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX + nodeName)
                .setFetchSource(true));
        if (!getResponse.isExists()) {
            return null;
        }
        Node node = this.nodeConverter.read(getResponse.getSourceAsString());
        String result;
        try (IIBNodeConnection nodeConnection = createIIBConnectionInstance(node)) {
            nodeConnection.connect();
            nodeConnection.setSynchronous(240000);
            IIBIntegrationServer integrationServer = nodeConnection.getServerByName(serverName);
            if (integrationServer == null) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("Integration server '" + serverName + "' not found. Not updating monitoring.");
                }
                throw new EtmException(EtmException.IIB_UNKNOWN_OBJECT);
            }
            if ("application".equals(objectType)) {
                result = updateApplicationMonitorning(nodeConnection, integrationServer, valueMap);
            } else if ("library".equals(objectType)) {
                String libraryName = getString("name", valueMap);
                IIBLibrary library = integrationServer.getSharedLibraryByName(libraryName);
                if (library == null) {
                    if (log.isDebugLevelEnabled()) {
                        log.logDebugMessage("Library '" + libraryName + "' not found. Not updating monitoring.");
                    }
                    throw new EtmException(EtmException.IIB_UNKNOWN_OBJECT);
                }
                result = updateLibraryMonitorning(nodeConnection, integrationServer, null, library.getVersion(), library, valueMap);
            } else if ("flow".equals(objectType)) {
                String flowName = getString("name", valueMap);
                IIBMessageFlow messageFlow = integrationServer.getMessageFlowByName(flowName);
                if (messageFlow == null) {
                    if (log.isDebugLevelEnabled()) {
                        log.logDebugMessage("Message flow '" + flowName + "' not found. Not updating monitoring.");
                    }
                    throw new EtmException(EtmException.IIB_UNKNOWN_OBJECT);
                }
                result = updateFlowMonitoring(nodeConnection, integrationServer, null, null, null, messageFlow, valueMap);
            } else {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("Unknown object type '" + objectType + "' not found. Not updating monitoring.");
                }
                throw new EtmException(EtmException.IIB_UNKNOWN_OBJECT);
            }
        } catch (ConfigManagerProxyLoggedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
        return result;
    }

    @GET
    @Path("/node/{nodeName}/server/{serverName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.IIB_EVENT_READ, SecurityRoles.IIB_EVENT_READ_WRITE})
    public String getServerDeployments(@PathParam("nodeName") String nodeName, @PathParam("serverName") String serverName) {
        var getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX + nodeName)
                .setFetchSource(true));
        if (!getResponse.isExists()) {
            return null;
        }
        var node = this.nodeConverter.read(getResponse.getSourceAsString());
        var builder = new JsonBuilder();
        builder.startObject();
        builder.startObject("deployments");
        try (IIBNodeConnection nodeConnection = createIIBConnectionInstance(node)) {
            nodeConnection.connect();
            IIBIntegrationServer integrationServer = nodeConnection.getServerByName(serverName);
            List<IIBSubFlow> sharedLibrarySubFlows = new ArrayList<>();
            List<IIBLibrary> sharedLibraries = integrationServer.getSharedLibraries();
            for (IIBLibrary library : sharedLibraries) {
                sharedLibrarySubFlows.addAll(library.getSubFlows());
            }

            List<IIBApplication> applications = integrationServer.getApplications();
            builder.startArray("applications");
            for (IIBApplication application : applications) {
                builder.startObject();
                List<IIBSubFlow> subFlows = application.getSubFlows();
                subFlows.addAll(sharedLibrarySubFlows);
                builder.field("name", application.getName());
                List<IIBLibrary> libraries = application.getLibraries();
                builder.startArray("libraries");
                for (IIBLibrary library : libraries) {
                    subFlows.addAll(library.getSubFlows());
                    addLibraryDeployment(nodeConnection, library, builder);
                }
                builder.endArray();
                builder.startArray("flows");
                List<IIBMessageFlow> messageFlows = application.getMessageFlows();
                for (IIBMessageFlow messageFlow : messageFlows) {
                    addFlowDeployment(nodeConnection, messageFlow, subFlows, builder);
                }
                builder.endArray().endObject();
            }
            builder.endArray();
            builder.startArray("flows");
            List<IIBMessageFlow> messageFlows = integrationServer.getMessageFlows();
            for (IIBMessageFlow messageFlow : messageFlows) {
                addFlowDeployment(nodeConnection, messageFlow, Collections.emptyList(), builder);
            }
            builder.endArray();
        }
        builder.endObject().endObject();
        return builder.build();
    }

    private void addLibraryDeployment(IIBNodeConnection nodeConnection, IIBLibrary library, JsonBuilder builder) {
        builder.startObject();
        builder.field("name", library.getName());
        builder.startArray("flows");
        List<IIBMessageFlow> messageFlows = library.getMessageFlows();
        for (IIBMessageFlow messageFlow : messageFlows) {
            addFlowDeployment(nodeConnection, messageFlow, library.getSubFlows(), builder);
        }
        builder.endArray().endObject();
    }

    private void addFlowDeployment(IIBNodeConnection nodeConnection, IIBMessageFlow flow, List<IIBSubFlow> subFlows, JsonBuilder builder) {
        boolean monitoringActivated = flow.isMonitoringActivated();
        String currentProfile = null;
        String currentProfileName = flow.getMonitoringProfileName();
        if (currentProfileName != null) {
            ConfigurableService configurableService = nodeConnection.getConfigurableService(MONITORING_PROFILES, currentProfileName);
            if (configurableService != null) {
                currentProfile = configurableService.getProperties().getProperty(PROFILE_PROPERTIES);
            }
        }
        builder.startObject();
        builder.field("name", flow.getName());
        builder.field("monitoring_active", monitoringActivated);
        builder.startArray("nodes");
        List<IIBNode> nodes = flow.getNodes();
        appendNodes(builder, null, nodes, subFlows, currentProfile);
        builder.endArray().endObject();
    }

    private void appendNodes(JsonBuilder builder, String namePrefix, List<IIBNode> nodes, List<IIBSubFlow> subFlows, String currentMonitoringProfile) {
        for (IIBNode node : nodes) {
            if (!node.isSupported()) {
                continue;
            }
            if ("SubFlowNode".equals(node.getType())) {
                String prop = node.getProperty("subflowImplFile");
                if (prop == null) {
                    continue;
                }
                final String subFlowName = prop.substring(0, prop.length() - ".subflow".length());
                Optional<IIBSubFlow> optional = subFlows.stream().filter(p -> p.getName().equals(subFlowName)).findFirst();
                if (optional.isEmpty()) {
                    continue;
                }
                List<IIBNode> subFlowNodes = optional.get().getNodes();
                appendNodes(builder, namePrefix == null ? node.getName() + "." : namePrefix + node.getName() + ".", subFlowNodes, subFlows, currentMonitoringProfile);
            } else {
                String fullNodeName = namePrefix == null ? node.getName() : namePrefix + node.getName();
                appendNode(builder, fullNodeName, node.getType(), isMonitoringSetInProfile(currentMonitoringProfile, fullNodeName));
            }
        }
    }

    private void appendNode(JsonBuilder builder, String name, String type, boolean monitoringSet) {
        builder.startObject();
        builder.field("name", name);
        builder.field("type", type);
        builder.field("monitoring_set", monitoringSet);
        builder.endObject();
    }

    private String updateApplicationMonitorning(IIBNodeConnection nodeConnection, IIBIntegrationServer integrationServer, Map<String, Object> valueMap) throws ConfigManagerProxyLoggedException {
        String applicationName = getString("name", valueMap);
        IIBApplication application = integrationServer.getApplicationByName(applicationName);
        if (application == null) {
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Application '" + applicationName + "' not found. Not updating monitoring.");
            }
            throw new EtmException(EtmException.IIB_UNKNOWN_OBJECT);
        }
        String version = application.getVersion();
        List<Map<String, Object>> librariesValues = getArray("libraries", valueMap);
        for (Map<String, Object> libraryValues : librariesValues) {
            String libraryName = getString("name", libraryValues);
            IIBLibrary library = application.getLibraryByName(libraryName);
            if (library == null) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("Library '" + libraryName + "' not found. Not updating monitoring.");
                }
                throw new EtmException(EtmException.IIB_UNKNOWN_OBJECT);
            }
            updateLibraryMonitorning(nodeConnection, integrationServer, applicationName, version, library, libraryValues);
        }
        List<Map<String, Object>> flowsValues = getArray("flows", valueMap);
        for (Map<String, Object> flowValues : flowsValues) {
            String flowName = getString("name", flowValues);
            IIBMessageFlow messageFlow = application.getMessageFlowByName(flowName);
            if (messageFlow == null) {
                continue;
            }
            updateFlowMonitoring(nodeConnection, integrationServer, applicationName, null, version, messageFlow, flowValues);
        }
        return "{\"status\":\"success\"}";
    }

    private String updateLibraryMonitorning(IIBNodeConnection nodeConnection, IIBIntegrationServer integrationServer, String applicationName, String version, IIBLibrary library, Map<String, Object> valueMap) throws ConfigManagerProxyLoggedException {
        List<Map<String, Object>> flowsValues = getArray("flows", valueMap);
        for (Map<String, Object> flowValues : flowsValues) {
            String flowName = getString("name", flowValues);
            IIBMessageFlow messageFlow = library.getMessageFlowByName(flowName);
            if (messageFlow == null) {
                continue;
            }
            boolean monitoringActive = getBoolean("monitoring_active", flowValues);
            if (monitoringActive) {
                activateMonitoring(nodeConnection, integrationServer, applicationName, library.getName(), version, messageFlow, flowValues);
            } else {
                deactivateMonitoring(nodeConnection, messageFlow);
            }
        }
        return "{\"status\":\"success\"}";
    }


    private String updateFlowMonitoring(IIBNodeConnection nodeConnection, IIBIntegrationServer integrationServer, String applicationName, String libraryName, String version, IIBMessageFlow flow, Map<String, Object> valueMap) throws ConfigManagerProxyLoggedException {
        boolean monitoringActive = getBoolean("monitoring_active", valueMap);
        if (monitoringActive) {
            activateMonitoring(nodeConnection, integrationServer, applicationName, libraryName, version, flow, valueMap);
        } else {
            deactivateMonitoring(nodeConnection, flow);
        }
        return "{\"status\":\"success\"}";
    }

    private void activateMonitoring(IIBNodeConnection nodeConnection, IIBIntegrationServer integrationServer, String application, String library, String version, IIBMessageFlow flow, Map<String, Object> flowValues) throws ConfigManagerProxyLoggedException {
        if (log.isInfoLevelEnabled()) {
            log.logInfoMessage("Activating monitoring on '" + flow.getName() + "'.");
        }
        StringBuilder configurableServiceName = new StringBuilder("etm-" + nodeConnection.getNode().getName() + "_" + integrationServer.getName());
        if (application != null) {
            configurableServiceName.append("_").append(application);
        }
        if (library != null) {
            configurableServiceName.append("_").append(library);
        }
        configurableServiceName.append("_").append(flow.getName());
        ConfigurableService configurableService = nodeConnection.getConfigurableService(MONITORING_PROFILES, configurableServiceName.toString());
        if (configurableService == null) {
            nodeConnection.createConfigurableService(MONITORING_PROFILES, configurableServiceName.toString());
            configurableService = nodeConnection.getConfigurableService(MONITORING_PROFILES, configurableServiceName.toString());
        }
        String currentMonitoringProfile = configurableService.getProperties().getProperty(PROFILE_PROPERTIES);
        String newMonitoringProfile = createMonitoringProfile(application, library, version, flowValues);
        if (currentMonitoringProfile == null || !currentMonitoringProfile.equals(newMonitoringProfile)) {
            configurableService.setProperty(PROFILE_PROPERTIES, newMonitoringProfile);
        }
        flow.activateMonitoringProfile(configurableServiceName.toString());
    }

    private void deactivateMonitoring(IIBNodeConnection nodeConnection, IIBMessageFlow flow) {
        if (log.isInfoLevelEnabled()) {
            log.logInfoMessage("Deactivating monitoring on '" + flow.getName() + "'.");
        }
        String currentProfile = flow.deactivateMonitoringProfile();
        if (currentProfile != null) {
            nodeConnection.deleteConfigurableService(MONITORING_PROFILES, currentProfile);
        }
    }

    private String createMonitoringProfile(String application, String library, String version, Map<String, Object> flowValues) {
        MonitoringProfileBuilder builder = new MonitoringProfileBuilder();
        List<Map<String, Object>> nodesValues = getArray("nodes", flowValues);
        for (Map<String, Object> nodeValues : nodesValues) {
            boolean monitoringSet = getBoolean("monitoring_set", nodeValues);
            if (!monitoringSet) {
                continue;
            }
            String nodeName = getString("name", nodeValues);
            String nodeType = getString("type", nodeValues);
            if (application != null) {
                builder.addNode(nodeName, nodeType, application, version);
            } else if (library != null) {
                builder.addNode(nodeName, nodeType, library, version);
            } else {
                builder.addNode(nodeName, nodeType, null, null);
            }
        }
        return builder.build();
    }

    private boolean isMonitoringSetInProfile(String profile, String nodeName) {
        return profile != null && profile.contains("profile:eventSourceAddress=\"" + nodeName + ".");
    }

    private IIBNodeConnection createIIBConnectionInstance(Node node) {
        if (IIBApi.IIB_V10_ON_CLASSPATH) {
            try {
                Class<?> clazz = Class.forName("com.jecstar.etm.gui.rest.services.iib.proxy.v10.IIBNodeConnectionV10Impl");
                Constructor<?> constructor = clazz.getConstructor(Node.class);
                return (IIBNodeConnection) constructor.newInstance(node);
            } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
            }
        } else {
            try {
                Class<?> clazz = Class.forName("com.jecstar.etm.gui.rest.services.iib.proxy.v9.IIBNodeConnectionV9Impl");
                Constructor<?> constructor = clazz.getConstructor(Node.class);
                return (IIBNodeConnection) constructor.newInstance(node);
            } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
            }
        }
    }

    /**
     * Store a <code>ConfigurationChangedAuditLog</code> instance.
     *
     * @param auditLogBuilder The builder that contains the data.
     */
    private void storeConfigurationChangedAuditLog(ConfigurationChangedAuditLogBuilder auditLogBuilder) {
        var now = Instant.now();
        IndexRequestBuilder indexRequestBuilder = requestEnhancer.enhance(
                new IndexRequestBuilder(ElasticsearchLayout.AUDIT_LOG_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(now))
                        .setId(auditLogBuilder.getId())
        )
                .setSource(this.configurationChangedAuditLogConverter.write(
                        auditLogBuilder
                                .setId(idGenerator.createId())
                                .setTimestamp(now)
                                .build()
                ), XContentType.JSON);
        dataRepository.indexAsync(indexRequestBuilder, DataRepository.noopActionListener());
    }
}
