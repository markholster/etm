package com.jecstar.etm.gui.rest.services.iib;

import com.ibm.broker.config.proxy.ConfigManagerProxyLoggedException;
import com.ibm.broker.config.proxy.ConfigurableService;
import com.jecstar.etm.gui.rest.AbstractGuiService;
import com.jecstar.etm.gui.rest.IIBApi;
import com.jecstar.etm.gui.rest.services.iib.proxy.*;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

    private static Client client;
    private static EtmConfiguration etmConfiguration;

    private final NodeConverterJsonImpl nodeConverter = new NodeConverterJsonImpl();

    public static void initialize(Client client, EtmConfiguration etmConfiguration) {
        IIBService.client = client;
        IIBService.etmConfiguration = etmConfiguration;
    }

    @GET
    @Path("/nodes")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.IIB_NODE_READ, SecurityRoles.IIB_NODE_READ_WRITE})
    public String getNodes() {
        SearchRequestBuilder searchRequestBuilder = enhanceRequest(client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME), etmConfiguration)
                .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE).setFetchSource(true)
                .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE));
        ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
        if (!scrollableSearch.hasNext()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        result.append("{\"nodes\": [");
        boolean first = true;
        for (SearchHit searchHit : scrollableSearch) {
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
        enhanceRequest(
                client.prepareDelete(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX + nodeName),
                etmConfiguration
        ).get();
        return "{\"status\":\"success\"}";
    }

    @PUT
    @Path("/node/{nodeName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.IIB_NODE_READ_WRITE)
    public String addNode(@PathParam("nodeName") String nodeName, String json) {
        Node node = this.nodeConverter.read(toStringWithNamespace(json, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE));
        try (IIBNodeConnection nodeConnection = createIIBConnectionInstance(node)) {
            nodeConnection.connect();
        }
        enhanceRequest(
                client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX + nodeName),
                etmConfiguration
        )
                .setDoc(this.nodeConverter.write(node), XContentType.JSON)
                .setDocAsUpsert(true)
                .setDetectNoop(true)
                .get();
        return "{ \"status\": \"success\" }";
    }

    @GET
    @Path("/node/{nodeName}/servers")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.IIB_EVENT_READ, SecurityRoles.IIB_EVENT_READ_WRITE})
    public String getServers(@PathParam("nodeName") String nodeName) {
        GetResponse getResponse = client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX + nodeName)
                .setFetchSource(true).get();
        if (!getResponse.isExists()) {
            return null;
        }
        Node node = this.nodeConverter.read(getResponse.getSourceAsString());
        StringBuilder result = new StringBuilder();
        result.append("{\"servers\": [");
        boolean first = true;
        try (IIBNodeConnection nodeConnection = createIIBConnectionInstance(node)) {
            nodeConnection.connect();
            List<IIBIntegrationServer> integrationServers = nodeConnection.getServers();
            for (IIBIntegrationServer integrationServer : integrationServers) {
                if (!first) {
                    result.append(",");
                }
                result.append(escapeToJson(integrationServer.getName(), true));
                first = false;
            }
        }
        result.append("]}");
        return result.toString();
    }

    @POST
    @Path("/node/{nodeName}/server/{serverName}/{objectType}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(SecurityRoles.IIB_EVENT_READ_WRITE)
    public String updateEventMonitoring(@PathParam("nodeName") String nodeName, @PathParam("serverName") String serverName, @PathParam("objectType") String objectType, String json) {
        Map<String, Object> valueMap = toMap(json);
        GetResponse getResponse = client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX + nodeName)
                .setFetchSource(true).get();
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
                result = updateFlowMonitorning(nodeConnection, integrationServer, null, null, null, messageFlow, valueMap);
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
        GetResponse getResponse = client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX + nodeName)
                .setFetchSource(true).get();
        if (!getResponse.isExists()) {
            return null;
        }
        Node node = this.nodeConverter.read(getResponse.getSourceAsString());
        StringBuilder result = new StringBuilder();
        result.append("{\"deployments\": {");
        try (IIBNodeConnection nodeConnection = createIIBConnectionInstance(node)) {
            nodeConnection.connect();
            IIBIntegrationServer integrationServer = nodeConnection.getServerByName(serverName);
            List<IIBSubFlow> sharedLibrarySubFlows = new ArrayList<>();
            List<IIBLibrary> sharedLibraries = integrationServer.getSharedLibraries();
            for (IIBLibrary library : sharedLibraries) {
                sharedLibrarySubFlows.addAll(library.getSubFlows());
            }

            List<IIBApplication> applications = integrationServer.getApplications();
            result.append("\"applications\": [");
            boolean firstApplication = true;
            for (IIBApplication application : applications) {
                if (!firstApplication) {
                    result.append(",");
                }
                result.append("{");
                List<IIBSubFlow> subFlows = application.getSubFlows();
                subFlows.addAll(sharedLibrarySubFlows);
                addStringElementToJsonBuffer("name", application.getName(), result, true);
                List<IIBLibrary> libraries = application.getLibraries();
                result.append(", \"libraries\": [");
                boolean firstLibrary = true;
                for (IIBLibrary library : libraries) {
                    subFlows.addAll(library.getSubFlows());
                    if (!firstLibrary) {
                        result.append(",");
                    }
                    addLibraryDeployment(nodeConnection, library, result);
                    firstLibrary = false;
                }
                result.append("], \"flows\": [");
                List<IIBMessageFlow> messageFlows = application.getMessageFlows();
                boolean firstFlow = true;
                for (IIBMessageFlow messageFlow : messageFlows) {
                    if (!firstFlow) {
                        result.append(",");
                    }
                    addFlowDeployment(nodeConnection, messageFlow, subFlows, result);
                    firstFlow = false;
                }
                result.append("]}");
                firstApplication = false;
            }
            result.append("], \"flows\": [");
            List<IIBMessageFlow> messageFlows = integrationServer.getMessageFlows();
            boolean firstFlow = true;
            for (IIBMessageFlow messageFlow : messageFlows) {
                if (!firstFlow) {
                    result.append(",");
                }
                addFlowDeployment(nodeConnection, messageFlow, Collections.emptyList(), result);
                firstFlow = false;
            }
            result.append("]");
        }
        result.append("}}");
        return result.toString();
    }

    private void addLibraryDeployment(IIBNodeConnection nodeConnection, IIBLibrary library, StringBuilder result) {
        result.append("{");
        addStringElementToJsonBuffer("name", library.getName(), result, true);
        result.append(", \"flows\": [");
        List<IIBMessageFlow> messageFlows = library.getMessageFlows();
        boolean firstFlow = true;
        for (IIBMessageFlow messageFlow : messageFlows) {
            if (!firstFlow) {
                result.append(",");
            }
            addFlowDeployment(nodeConnection, messageFlow, library.getSubFlows(), result);
            firstFlow = false;
        }
        result.append("]}");
    }

    private void addFlowDeployment(IIBNodeConnection nodeConnection, IIBMessageFlow flow, List<IIBSubFlow> subFlows, StringBuilder result) {
        boolean monitoringActivated = flow.isMonitoringActivated();
        String currentProfile = null;
        String currentProfileName = flow.getMonitoringProfileName();
        if (currentProfileName != null) {
            ConfigurableService configurableService = nodeConnection.getConfigurableService(MONITORING_PROFILES, currentProfileName);
            if (configurableService != null) {
                currentProfile = configurableService.getProperties().getProperty(PROFILE_PROPERTIES);
            }
        }
        result.append("{");
        addStringElementToJsonBuffer("name", flow.getName(), result, true);
        addBooleanElementToJsonBuffer("monitoring_active", monitoringActivated, result, false);
        result.append(", \"nodes\": [");
        List<IIBNode> nodes = flow.getNodes();
        appendNodes(result, null, nodes, subFlows, currentProfile, true);
        result.append("]}");
    }

    private boolean appendNodes(StringBuilder buffer, String namePrefix, List<IIBNode> nodes, List<IIBSubFlow> subFlows, String currentMonitoringProfile, boolean firstNode) {
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
                if (!optional.isPresent()) {
                    continue;
                }
                List<IIBNode> subFlowNodes = optional.get().getNodes();
                firstNode = appendNodes(buffer, namePrefix == null ? node.getName() + "." : namePrefix + node.getName() + ".", subFlowNodes, subFlows, currentMonitoringProfile, firstNode);
            } else {
                if (!firstNode) {
                    buffer.append(",");
                }
                String fullNodeName = namePrefix == null ? node.getName() : namePrefix + node.getName();
                appendNode(buffer, fullNodeName, node.getType(), isMonitoringSetInProfile(currentMonitoringProfile, fullNodeName));
                firstNode = false;
            }
        }
        return firstNode;
    }

    private void appendNode(StringBuilder buffer, String name, String type, boolean monitoringSet) {
        buffer.append("{");
        addStringElementToJsonBuffer("name", name, buffer, true);
        addStringElementToJsonBuffer("type", type, buffer, false);
        addBooleanElementToJsonBuffer("monitoring_set", monitoringSet, buffer, false);
        buffer.append("}");
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
            updateFlowMonitorning(nodeConnection, integrationServer, applicationName, null, version, messageFlow, flowValues);
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


    private String updateFlowMonitorning(IIBNodeConnection nodeConnection, IIBIntegrationServer integrationServer, String applicationName, String libraryName, String version, IIBMessageFlow flow, Map<String, Object> valueMap) throws ConfigManagerProxyLoggedException {
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
        return profile != null && profile.indexOf("profile:eventSourceAddress=\"" + nodeName + ".") >= 0;
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


}
