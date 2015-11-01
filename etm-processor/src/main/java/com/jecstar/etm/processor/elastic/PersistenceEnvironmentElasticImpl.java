package com.jecstar.etm.processor.elastic;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesAction;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateAction;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverterTags;
import com.jecstar.etm.core.domain.converter.json.TelemetryEventConverterTagsJsonImpl;
import com.jecstar.etm.processor.metrics.MetricConverterTags;
import com.jecstar.etm.processor.metrics.MetricConverterTagsJsonImpl;
import com.jecstar.etm.processor.processor.PersistenceEnvironment;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;

public class PersistenceEnvironmentElasticImpl implements PersistenceEnvironment {

	private final EtmConfiguration etmConfiguration;
	private final Client elasticClient;
	private final TelemetryEventConverterTags eventTags = new TelemetryEventConverterTagsJsonImpl();
	private final MetricConverterTags metricTags = new MetricConverterTagsJsonImpl();

	public PersistenceEnvironmentElasticImpl(final EtmConfiguration etmConfiguration, final Client elasticClient) {
		this.etmConfiguration = etmConfiguration;
		this.elasticClient = elasticClient;
	}
	
	@Override
	public TelemetryEventRepository createTelemetryEventRepository(final MetricRegistry metricRegistry) {
		return new TelemetryEventRepositoryElasticImpl(this.etmConfiguration, this.elasticClient, metricRegistry);
	}
	
	@Override
	public ScheduledReporter createMetricReporter(String nodeName, MetricRegistry metricRegistry) {
		return new MetricReporterElasticImpl(metricRegistry, nodeName, this.elasticClient);
	}


	@Override
	public void createEnvironment() {
		// TODO add template for EtmConfiguration?
		new PutIndexTemplateRequestBuilder(this.elasticClient, PutIndexTemplateAction.INSTANCE, "etm_event")
			.setCreate(false)
			.setTemplate("etm_event_*")
			.setSettings(Settings.settingsBuilder()
					.put("number_of_shards", this.etmConfiguration.getShardsPerIndex())
					.put("number_of_replicas", this.etmConfiguration.getReplicasPerIndex()))
			.addMapping("_default_", createEventMapping("_default_"))
			.addAlias(new Alias("etm_event_all"))
			.addAlias(new Alias("etm_event_today"))
			.get();
		new PutIndexTemplateRequestBuilder(this.elasticClient, PutIndexTemplateAction.INSTANCE, "etm_stats")
		.setCreate(false)
		.setTemplate("etm_stats_*")
		.setSettings(Settings.settingsBuilder()
				.put("number_of_shards", this.etmConfiguration.getShardsPerIndex())
				.put("number_of_replicas", this.etmConfiguration.getReplicasPerIndex()))
		.addMapping("_default_", createStatsMapping())
		.get();
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
	
	private String createStatsMapping() {
		return "{\"dynamic_templates\": ["
				+ "{ \"" + this.metricTags.getTimestampTag() + "\": { \"match\": \"" + this.metricTags.getTimestampTag() + "\", \"mapping\": {\"type\": \"date\", \"index\": \"not_analyzed\"}}}"
				+ ", { \"other\": { \"match\": \"*\", \"mapping\": {\"index\": \"not_analyzed\"}}}]}";	
	}

	
	private List<String> getIndicesFromAliasName(final ElasticsearchClient elasticSearchClient, final String aliasName) {
		GetAliasesResponse aliasesResponse = new GetAliasesRequestBuilder(elasticSearchClient, GetAliasesAction.INSTANCE, aliasName).get();
		ImmutableOpenMap<String, List<AliasMetaData>> aliases = aliasesResponse.getAliases();
	    final List<String> allIndices = new ArrayList<>();
	    aliases.keysIt().forEachRemaining(allIndices::add);
	    return allIndices;
	}

	@Override
	public void close() {
	}

}
