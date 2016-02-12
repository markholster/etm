package com.jecstar.etm.launcher;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesAction;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesRequestBuilder;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateAction;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.IndexTemplateAlreadyExistsException;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.EtmPrincipal;
import com.jecstar.etm.core.domain.converter.EtmConfigurationConverter;
import com.jecstar.etm.core.domain.converter.EtmPrincipalConverter;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverterTags;
import com.jecstar.etm.core.domain.converter.json.EtmConfigurationConverterJsonImpl;
import com.jecstar.etm.core.domain.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.core.domain.converter.json.TelemetryEventConverterTagsJsonImpl;
import com.jecstar.etm.launcher.http.BCrypt;
import com.jecstar.etm.processor.elastic.ElasticBackedEtmConfiguration;
import com.jecstar.etm.processor.metrics.MetricConverterTags;
import com.jecstar.etm.processor.metrics.MetricConverterTagsJsonImpl;

public class ElasticsearchIndextemplateCreator {
	
	private final TelemetryEventConverterTags eventTags = new TelemetryEventConverterTagsJsonImpl();
	private final MetricConverterTags metricTags = new MetricConverterTagsJsonImpl();
	private final EtmConfigurationConverter<String> etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
	private final EtmPrincipalConverter<String> etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();

	public void createTemplates(Client elasticClient) {
		try {
			GetIndexTemplatesResponse response = new GetIndexTemplatesRequestBuilder(elasticClient, GetIndexTemplatesAction.INSTANCE, "etm_event").get();
			if (response.getIndexTemplates() == null || response.getIndexTemplates().isEmpty()) {
				new PutIndexTemplateRequestBuilder(elasticClient, PutIndexTemplateAction.INSTANCE, "etm_event")
					.setCreate(true)
					.setTemplate("etm_event_*")
					.setSettings(Settings.settingsBuilder()
						.put("number_of_shards", 5)
						.put("number_of_replicas", 0))
					.addMapping("_default_", createEventMapping("_default_")).addAlias(new Alias("etm_event_all"))
					.get();
			}
		} catch (IndexTemplateAlreadyExistsException e) {}
		
		try {
			GetIndexTemplatesResponse response = new GetIndexTemplatesRequestBuilder(elasticClient, GetIndexTemplatesAction.INSTANCE, "etm_metrics").get();
			if (response.getIndexTemplates() == null || response.getIndexTemplates().isEmpty()) {
				new PutIndexTemplateRequestBuilder(elasticClient, PutIndexTemplateAction.INSTANCE, "etm_metrics")
					.setCreate(true)
					.setTemplate("etm_metrics_*")
					.setSettings(Settings.settingsBuilder()
						.put("number_of_shards", 1)
						.put("number_of_replicas", 0))
					.addMapping("_default_", createMetricsMapping())
					.get();
			}
		} catch (IndexTemplateAlreadyExistsException e) {}
		
		try {
			GetIndexTemplatesResponse response = new GetIndexTemplatesRequestBuilder(elasticClient, GetIndexTemplatesAction.INSTANCE, ElasticBackedEtmConfiguration.INDEX_NAME).get();
			if (response.getIndexTemplates() == null || response.getIndexTemplates().isEmpty()) {
				new PutIndexTemplateRequestBuilder(elasticClient, PutIndexTemplateAction.INSTANCE, ElasticBackedEtmConfiguration.INDEX_NAME)
					.setCreate(true)
					.setTemplate(ElasticBackedEtmConfiguration.INDEX_NAME)
					.setSettings(Settings.settingsBuilder()
						.put("number_of_shards", 1)
						.put("number_of_replicas", 0))
					.addMapping("_default_", createEtmConfigurationMapping())
					.get();
				insertDefaultEtmConfiguration(elasticClient);
				insertAdminUser(elasticClient);
			}
		} catch (IndexTemplateAlreadyExistsException e) {}
	}
	
	private String createEventMapping(String type) {
		// TODO moet dit misschien met een path_match i.p.v. een match? 
		return "{\"dynamic_templates\": ["
				+ "{ \"" + this.eventTags.getPayloadTag() + "\": { \"match\": \"" + this.eventTags.getPayloadTag() + "\", \"mapping\": {\"index\": \"analyzed\"}}}"
				+ ", { \"" + this.eventTags.getEndpointHandlerLocationTag() + "\": { \"match\": \"" + this.eventTags.getEndpointHandlerLocationTag() + "\", \"mapping\": {\"type\": \"geo_point\"}}}"
				+ ", { \"" + this.eventTags.getEndpointHandlerHandlingTimeTag() + "\": { \"match\": \"" + this.eventTags.getEndpointHandlerHandlingTimeTag() + "\", \"mapping\": {\"type\": \"date\", \"index\": \"not_analyzed\"}}}"
				+ ", { \"other\": { \"match\": \"*\", \"mapping\": {\"index\": \"not_analyzed\"}}}"
				+ "]}";
	}
	
	private String createMetricsMapping() {
		return "{\"dynamic_templates\": ["
				+ "{ \"" + this.metricTags.getTimestampTag() + "\": { \"match\": \"" + this.metricTags.getTimestampTag() + "\", \"mapping\": {\"type\": \"date\", \"index\": \"not_analyzed\"}}}"
				+ ", { \"other\": { \"match\": \"*\", \"mapping\": {\"index\": \"not_analyzed\"}}}]}";	
	}
	
	private String createEtmConfigurationMapping() {
		return "{\"dynamic_templates\": [{ \"other\": { \"match\": \"*\", \"mapping\": {\"index\": \"not_analyzed\"}}}]}";	
	}

	private void insertDefaultEtmConfiguration(Client elasticClient) {
		elasticClient.prepareIndex(ElasticBackedEtmConfiguration.INDEX_NAME, ElasticBackedEtmConfiguration.NODE_INDEX_TYPE, ElasticBackedEtmConfiguration.DEFAULT_ID)
			.setConsistencyLevel(WriteConsistencyLevel.ONE)
			.setSource(this.etmConfigurationConverter.convert(null, new EtmConfiguration("temp-for-creating-default", null)))
			.get();
	}
	
	private void insertAdminUser(Client elasticClient) {
		EtmPrincipal adminUser = new EtmPrincipal("admin", BCrypt.hashpw("password", BCrypt.gensalt()));
		adminUser.addRole("admin");
		elasticClient.prepareIndex(ElasticBackedEtmConfiguration.INDEX_NAME, "user", adminUser.getId())
			.setConsistencyLevel(WriteConsistencyLevel.ONE)
			.setSource(this.etmPrincipalConverter.convert(adminUser))
			.get();	
	}

}
