package com.jecstar.etm.gui.rest.repository.elastic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequestBuilder;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.monitor.jvm.JvmInfo;
import org.elasticsearch.monitor.os.OsInfo;
import org.elasticsearch.monitor.os.OsInfo.Cpu;
import org.elasticsearch.monitor.os.OsInfo.Mem;
import org.elasticsearch.monitor.os.OsInfo.Swap;

import com.jecstar.etm.core.converter.EtmConfigurationConverterTags;
import com.jecstar.etm.core.converter.json.EtmConfigurationConverterTagsJsonImpl;
import com.jecstar.etm.gui.rest.ClusterStatus;
import com.jecstar.etm.gui.rest.StatusCode;
import com.jecstar.etm.gui.rest.Node;
import com.jecstar.etm.gui.rest.NodeStatus;
import com.jecstar.etm.gui.rest.repository.NodeRepository;

public class NodeRepositoryElasticImpl implements NodeRepository {

	private final String indexName = "etm_configuration";
	private final String indexType = "node";
	private final String defaultId = "default_configuration";
	
	private final EtmConfigurationConverterTags tags = new EtmConfigurationConverterTagsJsonImpl();
	private final Client elasticClient;

	public NodeRepositoryElasticImpl(Client elasticClient) {
		this.elasticClient = elasticClient;
	}
	
	@Override
	public Map<String, Object> getNodeConfiguration(String nodeName) {
		GetResponse getResponse = this.elasticClient.prepareGet(this.indexName, this.indexType, this.defaultId).get();
		if (!getResponse.isExists()) {
			return Collections.emptyMap();
		}
		Map<String, Object> statusData = getResponse.getSourceAsMap();
		if (nodeName.equals("cluster")) {
			return statusData;
		}
		statusData.remove(this.tags.getLicenseTag());
		getResponse = this.elasticClient.prepareGet(this.indexName, this.indexType,  nodeName).get();
		if (getResponse.isExists()) {
			statusData.putAll(getResponse.getSourceAsMap());
		}
//		NodesInfoResponse nodesInfoResponse = new NodesInfoRequestBuilder(this.elasticClient.admin().cluster()).all().get(TimeValue.timeValueMillis(10000));
//		NodeInfo[] nodes = nodesInfoResponse.getNodes();
//		for (NodeInfo nodeInfo : nodes) {
//			if (nodeName.equals(nodeInfo.getNode().getName())) {
//				try {
//		            XContentBuilder builder = XContentFactory.jsonBuilder().prettyPrint();
//		            builder.startObject();
//		            nodeInfo.getOs().toXContent(builder, ToXContent.EMPTY_PARAMS);
//		            nodeInfo.getJvm().toXContent(builder, ToXContent.EMPTY_PARAMS);
//		            nodeInfo.getNetwork().toXContent(builder, ToXContent.EMPTY_PARAMS);
//		            nodeInfo.getProcess().toXContent(builder, ToXContent.EMPTY_PARAMS);
//		            builder.endObject();
//					statusData.put("status", builder.string());
//				} catch (Exception e) {
//					
//				}
//				break;
//			}
//		}
		return statusData;
	}

	@Override
	public List<Node> getNodes() {
		NodesInfoResponse nodesInfoResponse = new NodesInfoRequestBuilder(this.elasticClient.admin().cluster()).all().get(TimeValue.timeValueMillis(10000));
		NodeInfo[] nodes = nodesInfoResponse.getNodes();
		List<Node> result = new ArrayList<Node>();
		for (NodeInfo nodeInfo : nodes) {
			Node node = new Node();
			node.name = nodeInfo.getNode().getName();
			node.hostAddress = nodeInfo.getNode().getHostAddress();
			node.hostName = nodeInfo.getNode().getHostName();
			node.active = true;
			result.add(node);
		}
		// TODO haal de inactive configuraties uit de config database.
		return result;
	}

	@Override
	public void update(String nodeName, Map<String, Object> values) {
		if ("cluster".equals(nodeName)) {
			this.elasticClient.prepareUpdate(this.indexName, this.indexType, this.defaultId)
				.setConsistencyLevel(WriteConsistencyLevel.ONE)
				.setDetectNoop(true)
				.setDoc(values)
				.get();
			return;
		} else {
			GetResponse getResponse = this.elasticClient.prepareGet(this.indexName, this.indexType, this.defaultId).get();
			if (!getResponse.isExists()) {
				return;
			}
			Map<String, Object> clusterData = getResponse.getSourceAsMap();
			clusterData.remove(this.tags.getLicenseTag());
			Iterator<Map.Entry<String, Object>> entryIterator = values.entrySet().iterator();
			while(entryIterator.hasNext()) {
				Map.Entry<String, Object> entry = entryIterator.next();
			    String key = entry.getKey();
				if (clusterData.containsKey(key) && clusterData.get(key).equals(values.get(key))) {
					entryIterator.remove();
				}
			}
			if (values.size() == 0) {
				this.elasticClient.prepareDelete(this.indexName, this.indexType, nodeName)
					.setConsistencyLevel(WriteConsistencyLevel.ONE)
					.get();
			} else {
				this.elasticClient.prepareIndex(this.indexName, this.indexType, nodeName)
					.setConsistencyLevel(WriteConsistencyLevel.ONE)
					.setSource(values)
					.get();
				
			}
		}
	}

	@Override
	public ClusterStatus getClusterStatus() {
		ClusterStatus clusterStatus = new ClusterStatus();
		ClusterHealthResponse clusterHealthResponse = new ClusterHealthRequestBuilder(this.elasticClient.admin().cluster()).get();
		clusterStatus.clusterName = clusterHealthResponse.getClusterName();
		clusterStatus.statusCode = StatusCode.fromClusterHealthStatus(clusterHealthResponse.getStatus());
		clusterStatus.numberOfNodes =  clusterHealthResponse.getNumberOfNodes();
		clusterStatus.numberOfDataNodes = clusterHealthResponse.getNumberOfDataNodes();
		clusterStatus.numberOfActivePrimaryShards = clusterHealthResponse.getActivePrimaryShards();
		clusterStatus.numberOfActiveShards = clusterHealthResponse.getActiveShards();
		clusterStatus.numberOfRelocatingShards = clusterHealthResponse.getRelocatingShards();
		clusterStatus.numberOfInitializingShards = clusterHealthResponse.getInitializingShards();
		clusterStatus.numberOfUnassignedShards = clusterHealthResponse.getUnassignedShards();
		clusterStatus.numberOfDelayedUnassignedShards = clusterHealthResponse.getDelayedUnassignedShards();
		clusterStatus.numberOfPendingTasks = clusterHealthResponse.getNumberOfPendingTasks();
		clusterStatus.numberOfInFlightFethch = clusterHealthResponse.getNumberOfInFlightFetch();
		return clusterStatus;
	}

	@Override
	public NodeStatus getNodeStatus(String node) {
		NodesInfoResponse nodesInfoResponse = new NodesInfoRequestBuilder(this.elasticClient.admin().cluster())
				.setNodesIds(node)
				.get(TimeValue.timeValueMillis(10000));
		NodeStatus nodeStatus = new NodeStatus();
		if (nodesInfoResponse.getNodes() == null || nodesInfoResponse.getNodes().length < 1) {
			return nodeStatus;
		}
		NodeInfo nodeInfo = nodesInfoResponse.getNodes()[0];
		nodeStatus.id = nodeInfo.getNode().getId();
		nodeStatus.hostname = nodeInfo.getHostname();
		nodeStatus.address = nodeInfo.getNetwork().getPrimaryInterface().address();
		nodeStatus.client = nodeInfo.getNode().isClientNode();
		nodeStatus.data = nodeInfo.getNode().isDataNode();
		nodeStatus.master = nodeInfo.getNode().isMasterNode();
		
		OsInfo os = nodeInfo.getOs();
		if (os != null) {
			nodeStatus.osAvailableProcessors =  os.availableProcessors();
			nodeStatus.osRefreshInterval = os.getRefreshInterval();
			Cpu osCpu = os.cpu();
			if (osCpu != null) {
				nodeStatus.osCpuCacheSize =  osCpu.getCacheSize().bytes();
				nodeStatus.osCpuCoresPerSocket = osCpu.getCoresPerSocket();
				nodeStatus.osCpuMhz = osCpu.getMhz();
				nodeStatus.osCpuModel = osCpu.getModel();
				nodeStatus.osCpuTotalCores = osCpu.getTotalCores();
				nodeStatus.osCpuTotalSockets = osCpu.getTotalSockets();
				nodeStatus.osCpuVendor = osCpu.getVendor();
			}
			Mem osMem = os.getMem();
			if (osMem != null) {
				nodeStatus.osMemTotal = osMem.getTotal().bytes();
			}
			Swap osSwap = os.getSwap();
			if (osSwap != null) {
				nodeStatus.osSwapTotal = osSwap.getTotal().bytes();
			}
		}
		
		JvmInfo jvm = nodeInfo.getJvm();
		if (jvm != null) {
			nodeStatus.jvmStartTime = jvm.getStartTime();
			nodeStatus.jvmPid = jvm.getPid();
			nodeStatus.jvmName = jvm.getVmName();
			nodeStatus.jvmVendor =  jvm.getVmVendor();
			nodeStatus.jvmVersion =  jvm.getVmVersion();
			org.elasticsearch.monitor.jvm.JvmInfo.Mem jvmMem = jvm.mem();
			if (jvmMem != null) {
				nodeStatus.jvmMemDirectMax = jvmMem.getDirectMemoryMax().bytes();
				nodeStatus.jvmMemHeapInit = jvmMem.getHeapInit().bytes();
				nodeStatus.jvmMemHeapMax = jvmMem.getHeapMax().bytes();
				nodeStatus.jvmMemNonHeapInit = jvmMem.getNonHeapInit().getBytes();
				nodeStatus.jvmMemNonHeapMax = jvmMem.getNonHeapMax().getBytes();
			}
		}
		return nodeStatus;
	}

}
