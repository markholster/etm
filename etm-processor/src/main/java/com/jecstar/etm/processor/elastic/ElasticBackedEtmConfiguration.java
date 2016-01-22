package com.jecstar.etm.processor.elastic;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateAction;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.configuration.License;
import com.jecstar.etm.core.configuration.WriteConsistency;
import com.jecstar.etm.core.domain.converter.EtmConfigurationConverter;
import com.jecstar.etm.core.domain.converter.json.EtmConfigurationConverterJsonImpl;

public class ElasticBackedEtmConfiguration extends EtmConfiguration {

	private final String indexName = "etm_configuration";
	private final String nodeIndexType = "node";
	private final String licenseIndexType = "license";
	private final String defaultId = "default_configuration";
	private final Client elasticClient;
	private final EtmConfigurationConverter<String> etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
	
	private final long updateCheckInterval = 60 * 1000;
	private long lastCheckedForUpdates;
	
	public ElasticBackedEtmConfiguration(String nodeName, String component, final Client elasticClient) {
		super(nodeName, component);
		this.elasticClient = elasticClient;
		reloadConfigurationWhenNecessary();
	}
	
	@Override
	public License getLicense() {
		reloadConfigurationWhenNecessary();
		return super.getLicense();
	}

	@Override
	public int getEnhancingHandlerCount() {
		reloadConfigurationWhenNecessary();
		return super.getEnhancingHandlerCount();
	}
	
	@Override
	public int getPersistingHandlerCount() {
		reloadConfigurationWhenNecessary();
		return super.getPersistingHandlerCount();
	}
	
	@Override
	public int getEventBufferSize() {
		reloadConfigurationWhenNecessary();
		return super.getEventBufferSize();
	}

	@Override
	public int getPersistingBulkSize() {
		reloadConfigurationWhenNecessary();
		return super.getPersistingBulkSize();
	}
	
	@Override
	public int getPersistingBulkCount() {
		reloadConfigurationWhenNecessary();
		return super.getPersistingBulkCount();
	}
	
	@Override
	public int getPersistingBulkTime() {
		reloadConfigurationWhenNecessary();
		return super.getPersistingBulkTime();
	}
	
	@Override
	public int getShardsPerIndex() {
		reloadConfigurationWhenNecessary();
		return super.getShardsPerIndex();
	}
	
	@Override
	public int getReplicasPerIndex() {
		reloadConfigurationWhenNecessary();
		return super.getReplicasPerIndex();
	}

	@Override
	public int getMaxIndexCount() {
		reloadConfigurationWhenNecessary();
		return super.getMaxIndexCount();
	}
	
	@Override
	public WriteConsistency getWriteConsistency() {
		reloadConfigurationWhenNecessary();
		return super.getWriteConsistency();
	}
	
	private boolean reloadConfigurationWhenNecessary() {
		boolean changed = false;
		if (System.currentTimeMillis() - this.lastCheckedForUpdates <= this.updateCheckInterval) {
			return changed;
		}
		boolean hasIndex = this.elasticClient.admin().indices().exists(new IndicesExistsRequest(this.indexName)).actionGet().isExists();
		if (!hasIndex) {
			createDefault();
		}
		GetResponse defaultResponse = this.elasticClient.prepareGet(this.indexName, this.nodeIndexType, this.defaultId).get();
		if (!defaultResponse.isExists()) {
			// Should not happen, but if somehow the default configuration is lost, but the index isn't we create the new default over here.
			createDefault();
			defaultResponse = this.elasticClient.prepareGet(this.indexName, this.nodeIndexType, this.defaultId).get();
		}
		GetResponse nodeResponse = this.elasticClient.prepareGet(this.indexName, this.nodeIndexType, getNodeName()).get();
		GetResponse licenseResponse = this.elasticClient.prepareGet(this.indexName, this.licenseIndexType, this.defaultId).get();

		String defaultContent = defaultResponse.getSourceAsString();
		String nodeContent = null;

		if (nodeResponse.isExists()) {
			nodeContent = nodeResponse.getSourceAsString();
		}
		EtmConfiguration etmConfiguration = this.etmConfigurationConverter.convert(nodeContent, defaultContent, "temp-for-reload-merge", getComponent());
		if (licenseResponse.isExists() && !licenseResponse.isSourceEmpty()) {
			Object license = licenseResponse.getSourceAsMap().get(this.etmConfigurationConverter.getTags().getLicenseTag());
			if (license != null) {
				etmConfiguration.setLicenseKey(license.toString());
			}
		}
		changed = this.mergeAndNotify(etmConfiguration);
		this.lastCheckedForUpdates = System.currentTimeMillis();
		return changed;
	}
	
	private void createDefault() {
		new PutIndexTemplateRequestBuilder(this.elasticClient, PutIndexTemplateAction.INSTANCE, this.indexName)
			.setCreate(false)
			.setTemplate(this.indexName)
			.setSettings(Settings.settingsBuilder()
					.put("number_of_shards", 1)
					.put("number_of_replicas", 1))
			.addMapping("_default_", createMapping())
			.get();
		this.elasticClient.prepareIndex(this.indexName, this.nodeIndexType, this.defaultId)
			.setConsistencyLevel(WriteConsistencyLevel.ONE)
			.setSource(this.etmConfigurationConverter.convert(null, new EtmConfiguration("temp-for-creating-default", getComponent())))
			.get();
	}

	private String createMapping() {
		return "{\"dynamic_templates\": [{ \"other\": { \"match\": \"*\", \"mapping\": {\"index\": \"not_analyzed\"}}}]}";	
	}

}
