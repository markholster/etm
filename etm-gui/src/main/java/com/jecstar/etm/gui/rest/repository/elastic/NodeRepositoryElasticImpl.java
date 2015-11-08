package com.jecstar.etm.gui.rest.repository.elastic;

import java.util.Collections;
import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;

import com.jecstar.etm.gui.rest.repository.NodeRepository;

public class NodeRepositoryElasticImpl implements NodeRepository {

	private final String indexName = "etm_configuration";
	private final String indexType = "node";
	private final String defaultId = "default_configuration";
	
	private final Client elasticClient;

	public NodeRepositoryElasticImpl(Client elasticClient) {
		this.elasticClient = elasticClient;
	}
	
	@Override
	public Map<String, Object> getNodeConfiguration(String nodeName) {
		GetResponse getResponse = this.elasticClient.prepareGet(this.indexName, this.indexType, nodeName.equals("cluster") ? this.defaultId : nodeName).get();
		if (!getResponse.isExists()) {
			return Collections.emptyMap();
		}
		return getResponse.getSourceAsMap();
	}

}
