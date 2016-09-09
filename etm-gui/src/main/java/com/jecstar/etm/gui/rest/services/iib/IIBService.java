package com.jecstar.etm.gui.rest.services.iib;

import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.ibm.broker.config.proxy.ApplicationProxy;
import com.ibm.broker.config.proxy.BrokerProxy;
import com.ibm.broker.config.proxy.ConfigManagerProxyLoggedException;
import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.ConfigurableService;
import com.ibm.broker.config.proxy.ExecutionGroupProxy;
import com.ibm.broker.config.proxy.LibraryProxy;
import com.ibm.broker.config.proxy.MessageFlowProxy;
import com.jecstar.etm.gui.rest.AbstractJsonService;
import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

@Path("/iib")
public class IIBService extends AbstractJsonService {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(IIBService.class);

	public static final String RUNTIME_PROPERTY_MONITORING = "This/monitoring";
	public static final String RUNTIME_PROPERTY_MONITORING_PROFILE = "This/monitoringProfile";
	
	public static final String MONITORING_PROFILES = "MonitoringProfiles";
	public static final String PROFILE_PROPERTIES = "profileProperties";
	
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
	public String getParsers() {
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticSearchLayout.CONFIGURATION_INDEX_NAME)
				.setTypes(ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_IIB_NODE).setFetchSource(true)
				.setQuery(QueryBuilders.matchAllQuery())
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
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
			result.append(searchHit.getSourceAsString());
			first = false;
		}
		result.append("]}");
		return result.toString();
	}

	@DELETE
	@Path("/node/{nodeName}")
	@Produces(MediaType.APPLICATION_JSON)
	public String deleteParser(@PathParam("nodeName") String nodeName) {
		client.prepareDelete(ElasticSearchLayout.CONFIGURATION_INDEX_NAME,
				ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_IIB_NODE, nodeName)
				.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout())).get();
		return "{\"status\":\"success\"}";
	}

	@PUT
	@Path("/node/{nodeName}")
	@Produces(MediaType.APPLICATION_JSON)
	public String addParser(@PathParam("nodeName") String nodeName, String json) {
		Node node = this.nodeConverter.read(json);
		BrokerProxy bp = null;
		try {
			bp = node.connect();
		} finally {
			if (bp != null) {
				bp.disconnect();
			}
		}
		client.prepareUpdate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME,
				ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_IIB_NODE, nodeName).setDoc(this.nodeConverter.write(node))
				.setDocAsUpsert(true).setDetectNoop(true).setWaitForActiveShards(getActiveShardCount(etmConfiguration))
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
				.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount()).get();
		return "{ \"status\": \"success\" }";
	}

	@GET
	@Path("/node/{nodeName}/servers")
	@Produces(MediaType.APPLICATION_JSON)
	public String getServers(@PathParam("nodeName") String nodeName) {
		GetResponse getResponse = client.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME,
				ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_IIB_NODE, nodeName).setFetchSource(true).get();
		if (!getResponse.isExists()) {
			return null;
		}
		Node node = this.nodeConverter.read(getResponse.getSourceAsString());
		BrokerProxy bp = null;
		StringBuilder result = new StringBuilder();
		result.append("{\"servers\": [");
		boolean first = true;
		try {
			bp = node.connect();
			Enumeration<ExecutionGroupProxy> integrationServers = bp.getExecutionGroups(null);
			while (integrationServers.hasMoreElements()) {
				if (!first) {
					result.append(",");
				}
				result.append(escapeToJson(integrationServers.nextElement().getName(), true));
				first = false;
			}
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		} finally {
			if (bp != null) {
				bp.disconnect();
			}
		}
		result.append("]}");
		return result.toString();
	}

	@POST
	@Path("/node/{nodeName}/server/{serverName}/{objectType}")
	@Produces(MediaType.APPLICATION_JSON)
	public String updateApplicationMonitoring(@PathParam("nodeName") String nodeName, @PathParam("serverName") String serverName, @PathParam("objectType") String objectType, String json) {
		Map<String, Object> valueMap = toMap(json);
		GetResponse getResponse = client.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME,
				ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_IIB_NODE, nodeName).setFetchSource(true).get();
		if (!getResponse.isExists()) {
			return null;
		}
		Node node = this.nodeConverter.read(getResponse.getSourceAsString());
		BrokerProxy bp = null;
		String result = null;
		try {
			bp = node.connect();
			bp.setSynchronous(240000);
			ExecutionGroupProxy integrationServer = bp.getExecutionGroupByName(serverName);
			if (integrationServer == null) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Integration server '" + serverName + "' not found. Not updating monitoring.");
				}
				throw new EtmException(EtmException.IIB_UNKNOWN_OBJECT);
			}
			if ("application".equals(objectType)) {
				result = updateApplicationMonitorning(bp, integrationServer, valueMap);
			} else if ("library".equals(objectType)) {
				result = updateLibraryMonitorning(bp, integrationServer, valueMap);
			} else if ("flow".equals(objectType)) {
				result = updateFlowMonitorning(bp, integrationServer, valueMap);
			} else {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Unknown object type '" + objectType + "' not found. Not updating monitoring.");
				}
				throw new EtmException(EtmException.IIB_UNKNOWN_OBJECT);
			}
		} catch (ConfigManagerProxyPropertyNotInitializedException | IllegalArgumentException | ConfigManagerProxyLoggedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		} finally {
			if (bp != null) {
				bp.disconnect();
			}
		}
		return result;
	}
	
	@GET
	@Path("/node/{nodeName}/server/{serverName}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getServerDeployments(@PathParam("nodeName") String nodeName, @PathParam("serverName") String serverName) {
		GetResponse getResponse = client.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME,
				ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_IIB_NODE, nodeName).setFetchSource(true).get();
		if (!getResponse.isExists()) {
			return null;
		}
		Node node = this.nodeConverter.read(getResponse.getSourceAsString());
		BrokerProxy bp = null;
		StringBuilder result = new StringBuilder();
		result.append("{\"deployments\": {");
		try {
			bp = node.connect();
			ExecutionGroupProxy executionGroup = bp.getExecutionGroupByName(serverName);
			Enumeration<ApplicationProxy> applications = executionGroup.getApplications(null);
			result.append("\"applications\": [");
			boolean firstApplication = true;
			while (applications.hasMoreElements()) {
				if (!firstApplication) {
					result.append(",");
				}
				result.append("{");
				ApplicationProxy application = applications.nextElement();
				addStringElementToJsonBuffer("name", application.getName(), result, true);
				// TODO vanaf iib10 is de api voor subflows beschrikbaar. Vanaf dan wordt het handig om de libraries uit te lezen.
//				Enumeration<LibraryProxy> libraries = application.getLibraries(null);
//				result.append(", \"libraries\": [");
//				boolean firstLibrary = true;
//				while (libraries.hasMoreElements()) {
//					if (!firstLibrary) {
//						result.append(",");
//					}
//					addLibraryDeployment(libraries.nextElement(), result);
//					firstLibrary = false;
//				}
				result.append(", \"flows\": [");
				Enumeration<MessageFlowProxy> messageFlows = application.getMessageFlows(null);
				boolean firstFlow = true;
				while (messageFlows.hasMoreElements()) {
					MessageFlowProxy messageFlow = messageFlows.nextElement();
					if (!firstFlow) {
						result.append(",");
					}
					addMessageFlowDeployment(bp, messageFlow, result);
					firstFlow = false;
				}
				result.append("]}");
				firstApplication = false;
			}
			result.append("], \"libraries\": [");
			Enumeration<LibraryProxy> libraries = executionGroup.getLibraries(null);
			boolean firstLibrary = true;
			while (libraries.hasMoreElements()) {
				if (!firstLibrary) {
					result.append(",");
				}
				addLibraryDeployment(bp, libraries.nextElement(), result);
				firstLibrary = false;
			}
			result.append("], \"flows\": [");
			Enumeration<MessageFlowProxy> messageFlows = executionGroup.getMessageFlows(null);
			boolean firstFlow = true;
			while (messageFlows.hasMoreElements()) {
				MessageFlowProxy messageFlow = messageFlows.nextElement();
				if (!firstFlow) {
					result.append(",");
				}
				addMessageFlowDeployment(bp, messageFlow, result);
				firstFlow = false;
			}
			result.append("]");
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		} finally {
			if (bp != null) {
				bp.disconnect();
			}
		}
		result.append("}}");
		return result.toString();
	}
	
	private void addLibraryDeployment(BrokerProxy integrationNode, LibraryProxy library, StringBuilder result) throws ConfigManagerProxyPropertyNotInitializedException {
		result.append("{");
		addStringElementToJsonBuffer("name", library.getName(), result, true);
		result.append(", \"flows\": [");
		Enumeration<MessageFlowProxy> messageFlows = library.getMessageFlows(null);
		boolean firstFlow = true;
		while (messageFlows.hasMoreElements()) {
			if (!firstFlow) {
				result.append(",");
			}
			addMessageFlowDeployment(integrationNode, messageFlows.nextElement(), result);
			firstFlow = false;
		}
		result.append("]}");
	}

	private void addMessageFlowDeployment(BrokerProxy integrationNode, MessageFlowProxy messageFlow, StringBuilder result) throws ConfigManagerProxyPropertyNotInitializedException {
		boolean monitoringActivated = false;
		if (messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING) != null
				&& messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING)
						.equals("active")) {
			monitoringActivated = true;
		}		
		String currentProfile = null;
		String currentProfileName = messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING_PROFILE);
		if (currentProfileName != null) {
			ConfigurableService configurableService = integrationNode.getConfigurableService(MONITORING_PROFILES, currentProfileName);
			if (configurableService != null) {
				currentProfile = configurableService.getProperties().getProperty(PROFILE_PROPERTIES);
			}
		}
		result.append("{");
		addStringElementToJsonBuffer("name", messageFlow.getName(), result, true);
		addBooleanElementToJsonBuffer("monitoring_active", monitoringActivated, result, false);
		result.append(", \"nodes\": [");
		Enumeration<com.ibm.broker.config.proxy.MessageFlowProxy.Node> nodes = messageFlow.getNodes();
		boolean firstNode = true;
		while (nodes.hasMoreElements()) {
			com.ibm.broker.config.proxy.MessageFlowProxy.Node node = nodes.nextElement();
			if (!isSupportedNode(node)) {
				continue;
			}
			if (!firstNode) {
				result.append(",");
			}
			boolean monitoringSet = false;
			if (currentProfile != null) {
				monitoringSet = currentProfile.indexOf("profile:eventSourceAddress=\"" + node.getName()+ ".") >= 0;
			}
			result.append("{");
			addStringElementToJsonBuffer("name", node.getName(), result, true);
			addStringElementToJsonBuffer("type", node.getType(), result, false);
			addBooleanElementToJsonBuffer("monitoring_set", monitoringSet, result, false);
			result.append("}");
			firstNode = false;
		}
		result.append("]}");	}

	private boolean isSupportedNode(com.ibm.broker.config.proxy.MessageFlowProxy.Node node) {
		return node.getType().startsWith("ComIbmMQ")  && !"ComIbmMQHeaderNode".equals(node.getType());
	}
	
	private String updateApplicationMonitorning(BrokerProxy integrationNode, ExecutionGroupProxy integrationServer, Map<String, Object> valueMap) throws ConfigManagerProxyPropertyNotInitializedException, IllegalArgumentException, ConfigManagerProxyLoggedException {
		String applicationName = getString("name", valueMap);
		ApplicationProxy application = integrationServer.getApplicationByName(applicationName);
		if (application == null) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Application '" + applicationName + "' not found. Not updating monitoring.");
			}
			throw new EtmException(EtmException.IIB_UNKNOWN_OBJECT);
		}
		String version = application.getVersion();
		List<Map<String, Object>> flowsValues = getArray("flows", valueMap);
		for (Map<String, Object> flowValues : flowsValues) {
			String flowName = getString("name", flowValues);
			MessageFlowProxy messageFlow = application.getMessageFlowByName(flowName);
			if (messageFlow == null) {
				continue;
			}
			boolean monitoringActive = getBoolean("monitoring_active", flowValues);
			if (monitoringActive) {
				activateMonitoring(integrationNode, integrationServer, applicationName, null, version, messageFlow, flowValues);
			} else {
				deactivateMonitoring(integrationNode, messageFlow);
			}
			
		}
		return "{\"status\":\"success\"}";
	}
	
	private String updateLibraryMonitorning(BrokerProxy integrationNode, ExecutionGroupProxy integrationServer, Map<String, Object> valueMap) throws ConfigManagerProxyPropertyNotInitializedException, IllegalArgumentException, ConfigManagerProxyLoggedException {
		String libraryName = getString("name", valueMap);
		LibraryProxy library = integrationServer.getLibraryByName(libraryName);
		if (library == null) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Library '" + libraryName + "' not found. Not updating monitoring.");
			}
			throw new EtmException(EtmException.IIB_UNKNOWN_OBJECT);
		}
		String version = library.getVersion();
		List<Map<String, Object>> flowsValues = getArray("flows", valueMap);
		for (Map<String, Object> flowValues : flowsValues) {
			String flowName = getString("name", flowValues);
			MessageFlowProxy messageFlow = library.getMessageFlowByName(flowName);
			if (messageFlow == null) {
				continue;
			}
			boolean monitoringActive = getBoolean("monitoring_active", flowValues);
			if (monitoringActive) {
				activateMonitoring(integrationNode, integrationServer, null, libraryName, version, messageFlow, flowValues);
			} else {
				deactivateMonitoring(integrationNode, messageFlow);
			}
			
		}
		return "{\"status\":\"success\"}";
	}
	
	private String updateFlowMonitorning(BrokerProxy integrationNode, ExecutionGroupProxy integrationServer, Map<String, Object> valueMap) throws ConfigManagerProxyPropertyNotInitializedException, IllegalArgumentException, ConfigManagerProxyLoggedException {
		String flowName = getString("name", valueMap);
		MessageFlowProxy messageFlow = integrationServer.getMessageFlowByName(flowName);
		if (messageFlow == null) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Message flow '" + flowName + "' not found. Not updating monitoring.");
			}
			throw new EtmException(EtmException.IIB_UNKNOWN_OBJECT);
		}
		String version = messageFlow.getVersion();
		boolean monitoringActive = getBoolean("monitoring_active", valueMap);
		if (monitoringActive) {
			activateMonitoring(integrationNode, integrationServer, null, null, version, messageFlow, valueMap);
		} else {
			deactivateMonitoring(integrationNode, messageFlow);
		}
		return "{\"status\":\"success\"}";
	}

	private void activateMonitoring(BrokerProxy integrationNode, ExecutionGroupProxy integrationServer, String application, String library, String version, MessageFlowProxy messageFlow, Map<String, Object> flowValues) throws ConfigManagerProxyLoggedException, IllegalArgumentException, ConfigManagerProxyPropertyNotInitializedException {
		if (log.isInfoLevelEnabled()) {
			log.logInfoMessage("Activating monitoring on '" + messageFlow.getName() + "'.");
		}
		StringBuilder configurableServiceName = new StringBuilder("etm-" + integrationNode.getName() + "_" + integrationServer.getName());
		if (application != null) {
			configurableServiceName.append("_" + application);
		}
		if (library != null) {
			configurableServiceName.append("_" + library);
		}
		configurableServiceName.append(messageFlow.getName());
		ConfigurableService configurableService = integrationNode.getConfigurableService(MONITORING_PROFILES, configurableServiceName.toString());
		if (configurableService == null) {
			integrationNode.createConfigurableService(MONITORING_PROFILES, configurableServiceName.toString()); 
			configurableService = integrationNode.getConfigurableService(MONITORING_PROFILES, configurableServiceName.toString());
		}
		String currentMonitoringProfile = configurableService.getProperties().getProperty(PROFILE_PROPERTIES);
		String newMonitoringProfile = createMonitoringProfile(application, library, version, flowValues);
		if (currentMonitoringProfile == null || !currentMonitoringProfile.equals(newMonitoringProfile)) {
			configurableService.setProperty(PROFILE_PROPERTIES, newMonitoringProfile);
		}
		String csName = messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING_PROFILE);
		if (csName == null || !csName.equals(configurableServiceName)) {
			messageFlow.setRuntimeProperty(RUNTIME_PROPERTY_MONITORING_PROFILE, configurableServiceName.toString());
		}
		String status = messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING);
		if (status == null || !status.equals("active")) {
			messageFlow.setRuntimeProperty(RUNTIME_PROPERTY_MONITORING, "active");
		}	
	}

	private void deactivateMonitoring(BrokerProxy integrationNode, MessageFlowProxy messageFlow) throws ConfigManagerProxyPropertyNotInitializedException, IllegalArgumentException, ConfigManagerProxyLoggedException {
		if (log.isInfoLevelEnabled()) {
			log.logInfoMessage("Deactivating monitoring on '" + messageFlow.getName() + "'.");
		}
		String status = messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING);
		if ("active".equals(status)) {
			messageFlow.setRuntimeProperty(RUNTIME_PROPERTY_MONITORING, "inactive");
		}
		String currentProfile = messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING_PROFILE);
		if (currentProfile != null) {
			messageFlow.setRuntimeProperty(RUNTIME_PROPERTY_MONITORING_PROFILE, "");
			integrationNode.deleteConfigurableService(MONITORING_PROFILES, currentProfile);
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

	
}
