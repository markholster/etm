package com.jecstar.etm.processor.elastic;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.ImmutableSettings;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverterTags;
import com.jecstar.etm.core.domain.converter.json.TelemetryEventConverterTagsJsonImpl;
import com.jecstar.etm.processor.processor.PersistenceEnvironment;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;

public class PersistenceEnvironmentElasticImpl implements PersistenceEnvironment {

	private final EtmConfiguration etmConfiguration;
	private final Client elasticClient;
	private final TelemetryEventConverterTags eventTags = new TelemetryEventConverterTagsJsonImpl();

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
		new PutIndexTemplateRequestBuilder(this.elasticClient.admin().indices(), "etm_event")
			.setCreate(false)
			.setTemplate("etm_event_*")
			.setSettings(ImmutableSettings.settingsBuilder()
					.put("number_of_shards", this.etmConfiguration.getShardsPerIndex())
					.put("number_of_replicas", this.etmConfiguration.getReplicasPerIndex())
					.build())
			.addMapping("_default_", createEventMapping("_default_"))
			.addAlias(new Alias("etm_event_all"))
			.addAlias(new Alias("etm_event_today"))
			.get();
		new PutIndexTemplateRequestBuilder(this.elasticClient.admin().indices(), "etm_stats")
		.setCreate(false)
		.setTemplate("etm_stats_*")
		.setSettings(ImmutableSettings.settingsBuilder()
				.put("number_of_shards", this.etmConfiguration.getShardsPerIndex())
				.put("number_of_replicas", this.etmConfiguration.getReplicasPerIndex())
				.build())
		.addMapping("_default_", createStatsMapping())
		.get();
	}
	
	private String createEventMapping(String type) {
		// TODO moet dit misschien met een path_match i.p.v. een match? 
		return "{\"dynamic_templates\": ["
				+ "{ \"" + this.eventTags.getPayloadTag() + "\": { \"match\": \"" + this.eventTags.getPayloadTag() + "\", \"mapping\": {\"index\": \"analyzed\"}}}"
				+ "{ \"" + this.eventTags.getPayloadTag() + "\": { \"match\": \"" + this.eventTags.getEndpointHandlerLocationTag() + "\", \"mapping\": {\"type\": \"geo_point\"}}}"
				+ ", { \"other\": { \"match\": \"*\", \"mapping\": {\"index\": \"not_analyzed\"}}}"
				+ "]}";
	}
	
	private String createStatsMapping() {
		return "{\"dynamic_templates\": [{ \"other\": { \"match\": \"*\", \"mapping\": {\"index\": \"not_analyzed\"}}}]}";	
	}

	
	private List<String> getIndicesFromAliasName(final IndicesAdminClient indicesAdminClient, final String aliasName) {
		GetAliasesResponse aliasesResponse = new GetAliasesRequestBuilder(indicesAdminClient, aliasName).get();
		ImmutableOpenMap<String, List<AliasMetaData>> aliases = aliasesResponse.getAliases();
	    final List<String> allIndices = new ArrayList<>();
	    aliases.keysIt().forEachRemaining(allIndices::add);
	    return allIndices;
	}

	@Override
	public void close() {
	}

}
