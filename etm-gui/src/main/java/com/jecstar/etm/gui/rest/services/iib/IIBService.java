package com.jecstar.etm.gui.rest.services.iib;

import java.util.Enumeration;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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
import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.ExecutionGroupProxy;
import com.ibm.broker.config.proxy.LibraryProxy;
import com.ibm.broker.config.proxy.MessageFlowProxy;
import com.jecstar.etm.gui.rest.AbstractJsonService;
import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

@Path("/iib")
public class IIBService extends AbstractJsonService {

	public static final String RUNTIME_PROPERTY_MONITORING = "This/monitoring";
	public static final String RUNTIME_PROPERTY_MONITORING_PROFILE = "This/monitoringProfile";
	
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
					addMessageFlowDeployment(messageFlow, result);
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
				addLibraryDeployment(libraries.nextElement(), result);
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
				addMessageFlowDeployment(messageFlow, result);
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
	
	private void addLibraryDeployment(LibraryProxy library, StringBuilder result) throws ConfigManagerProxyPropertyNotInitializedException {
		result.append("{");
		addStringElementToJsonBuffer("name", library.getName(), result, true);
		result.append(", \"flows\": [");
		Enumeration<MessageFlowProxy> messageFlows = library.getMessageFlows(null);
		boolean firstFlow = true;
		while (messageFlows.hasMoreElements()) {
			if (!firstFlow) {
				result.append(",");
			}
			addMessageFlowDeployment(messageFlows.nextElement(), result);
			firstFlow = false;
		}
		result.append("]}");
	}

	private void addMessageFlowDeployment(MessageFlowProxy messageFlow, StringBuilder result) throws ConfigManagerProxyPropertyNotInitializedException {
		boolean monitoringActivated = false;
		if (messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING) != null
				&& messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING)
						.equals("active")) {
			monitoringActivated = true;
		}		
		String currentProfile =  messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING_PROFILE);
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
	
}
