package com.jecstar.etm.gui.rest.services.iib;

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

import com.ibm.broker.config.proxy.ConfigManagerProxyLoggedException;
import com.ibm.broker.config.proxy.ConfigurableService;
import com.jecstar.etm.gui.rest.AbstractJsonService;
import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBApplication;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBFlow;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBIntegrationServer;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBLibrary;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBMessageFlow;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBNode;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBNodeConnection;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBSubFlow;
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
		try (IIBNodeConnection nodeConnection = new IIBNodeConnection(node);) {
			nodeConnection.connect();
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
		StringBuilder result = new StringBuilder();
		result.append("{\"servers\": [");
		boolean first = true;
		try (IIBNodeConnection nodeConnection  = new IIBNodeConnection(node);) {
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
	public String updateApplicationMonitoring(@PathParam("nodeName") String nodeName, @PathParam("serverName") String serverName, @PathParam("objectType") String objectType, String json) {
		Map<String, Object> valueMap = toMap(json);
		GetResponse getResponse = client.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME,
				ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_IIB_NODE, nodeName).setFetchSource(true).get();
		if (!getResponse.isExists()) {
			return null;
		}
		Node node = this.nodeConverter.read(getResponse.getSourceAsString());
		String result = null;
		try (IIBNodeConnection nodeConnection = new IIBNodeConnection(node);) {
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
				IIBLibrary library = integrationServer.getLibraryByName(libraryName);
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
	public String getServerDeployments(@PathParam("nodeName") String nodeName, @PathParam("serverName") String serverName) {
		GetResponse getResponse = client.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME,
				ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_IIB_NODE, nodeName).setFetchSource(true).get();
		if (!getResponse.isExists()) {
			return null;
		}
		Node node = this.nodeConverter.read(getResponse.getSourceAsString());
		StringBuilder result = new StringBuilder();
		result.append("{\"deployments\": {");
		try (IIBNodeConnection nodeConnection = new IIBNodeConnection(node);) {
			nodeConnection.connect();
			IIBIntegrationServer integrationServer = nodeConnection.getServerByName(serverName);
			List<IIBApplication> applications = integrationServer.getApplications();
			result.append("\"applications\": [");
			boolean firstApplication = true;
			for (IIBApplication application : applications) {
				if (!firstApplication) {
					result.append(",");
				}
				result.append("{");
				addStringElementToJsonBuffer("name", application.getName(), result, true);
				List<IIBLibrary> libraries = application.getLibraries();
				result.append(", \"libraries\": [");
				boolean firstLibrary = true;
				for (IIBLibrary library : libraries) {
					if (!firstLibrary) {
						result.append(",");
					}
					addLibraryDeployment(nodeConnection, library, result);
					firstLibrary = false;
				}
				result.append(", \"flows\": [");
				List<IIBMessageFlow> messageFlows = application.getMessageFlows();
				boolean firstFlow = true;
				for (IIBMessageFlow messageFlow : messageFlows) {
					if (!firstFlow) {
						result.append(",");
					}
					addFlowDeployment(nodeConnection, messageFlow, result);
					firstFlow = false;
				}
				result.append(", \"subflows\": [");
				List<IIBSubFlow> subFlows = application.getSubFlows();
				firstFlow = true;
				for (IIBSubFlow subFlow : subFlows) {
					if (!firstFlow) {
						result.append(",");
					}
					addFlowDeployment(nodeConnection, subFlow, result);
					firstFlow = false;
				}
				result.append("]}");
				firstApplication = false;
			}
			result.append("], \"libraries\": [");
			List<IIBLibrary> libraries = integrationServer.getLibraries();
			boolean firstLibrary = true;
			for (IIBLibrary library : libraries) {
				if (!firstLibrary) {
					result.append(",");
				}
				addLibraryDeployment(nodeConnection, library, result);
				firstLibrary = false;
			}
			result.append("], \"flows\": [");
			List<IIBMessageFlow> messageFlows = integrationServer.getMessageFlows();
			boolean firstFlow = true;
			for (IIBMessageFlow messageFlow : messageFlows) {
				if (!firstFlow) {
					result.append(",");
				}
				addFlowDeployment(nodeConnection, messageFlow, result);
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
			addFlowDeployment(nodeConnection, messageFlow, result);
			firstFlow = false;
		}
		result.append("], \"subflows\": [");
		List<IIBSubFlow> subFlows = library.getSubFlows();
		firstFlow = true;
		for (IIBSubFlow subFlow : subFlows) {
			if (!firstFlow) {
				result.append(",");
			}
			addFlowDeployment(nodeConnection, subFlow, result);
			firstFlow = false;
		}
		result.append("]}");		
	}

	private void addFlowDeployment(IIBNodeConnection nodeConnection, IIBFlow flow, StringBuilder result) {
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
		boolean firstNode = true;
		for (IIBNode node : nodes) {
			if (!node.isSupported()) {
				continue;
			}
			if (!firstNode) {
				result.append(",");
			}
			result.append("{");
			addStringElementToJsonBuffer("name", node.getName(), result, true);
			addStringElementToJsonBuffer("type", node.getType(), result, false);
			addBooleanElementToJsonBuffer("monitoring_set", node.isMonitoringSetInProfile(currentProfile), result, false);
			result.append("}");
			firstNode = false;
		}
		result.append("]}");	}

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
			String libraryName = getString("name", valueMap);
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
			updateFlowMonitorning(nodeConnection, integrationServer, applicationName, null, version, messageFlow, valueMap);
		}
		List<Map<String, Object>> subflowsValues = getArray("subflows", valueMap);
		for (Map<String, Object> subflowValues : subflowsValues) {
			String flowName = getString("name", subflowValues);
			IIBSubFlow subFlow = application.getSubFlowByName(flowName);
			if (subFlow == null) {
				continue;
			}
			updateFlowMonitorning(nodeConnection, integrationServer, applicationName, null, version, subFlow, valueMap);
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
		List<Map<String, Object>> subflowsValues = getArray("subflows", valueMap);
		for (Map<String, Object> subflowValues : subflowsValues) {
			String flowName = getString("name", subflowValues);
			IIBSubFlow subFlow = library.getSubFlowByName(flowName);
			if (subFlow == null) {
				continue;
			}
			boolean monitoringActive = getBoolean("monitoring_active", subflowValues);
			if (monitoringActive) {
				activateMonitoring(nodeConnection, integrationServer, applicationName, library.getName(), version, subFlow, subflowValues);
			} else {
				deactivateMonitoring(nodeConnection, subFlow);
			}
		}		
		return "{\"status\":\"success\"}";
	}
	
	
	private String updateFlowMonitorning(IIBNodeConnection nodeConnection, IIBIntegrationServer integrationServer, String applicationName, String libraryName, String version, IIBFlow flow, Map<String, Object> valueMap) throws ConfigManagerProxyLoggedException {
		boolean monitoringActive = getBoolean("monitoring_active", valueMap);
		if (monitoringActive) {
			activateMonitoring(nodeConnection, integrationServer, applicationName, libraryName, version, flow, valueMap);
		} else {
			deactivateMonitoring(nodeConnection, flow);
		}
		return "{\"status\":\"success\"}";
	}

	private void activateMonitoring(IIBNodeConnection nodeConnection, IIBIntegrationServer integrationServer, String application, String library, String version, IIBFlow flow, Map<String, Object> flowValues) throws ConfigManagerProxyLoggedException {
		if (log.isInfoLevelEnabled()) {
			log.logInfoMessage("Activating monitoring on '" + flow.getName() + "'.");
		}
		StringBuilder configurableServiceName = new StringBuilder("etm-" + nodeConnection.getNode().getName() + "_" + integrationServer.getName());
		if (application != null) {
			configurableServiceName.append("_" + application);
		}
		if (library != null) {
			configurableServiceName.append("_" + library);
		}
		configurableServiceName.append("_" + flow.getName());
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

	private void deactivateMonitoring(IIBNodeConnection nodeConnection, IIBFlow flow) {
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

	
}
