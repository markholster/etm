package com.jecstar.etm.processor.jee.configurator.elastic;

import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateAction;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;

import com.jecstar.etm.core.configuration.ConfigurationChangeListener;
import com.jecstar.etm.core.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.converter.TelemetryEventConverterTags;
import com.jecstar.etm.core.converter.json.TelemetryEventConverterTagsJsonImpl;
import com.jecstar.etm.processor.processor.PersistenceEnvironment;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;
import com.jecstar.etm.processor.repository.elasticsearch.TelemetryEventRepositoryElasticImpl;

public class PersistenceEnvironmentElasticImpl implements PersistenceEnvironment, ConfigurationChangeListener {

	private final EtmConfiguration etmConfiguration;
	private final Client elasticClient;
	
	private final TelemetryEventConverterTags eventTags = new TelemetryEventConverterTagsJsonImpl();

	public PersistenceEnvironmentElasticImpl(final EtmConfiguration etmConfiguration, final Client elasticClient) {
		this.etmConfiguration = etmConfiguration;
		this.elasticClient = elasticClient;
		this.etmConfiguration.addConfigurationChangeListener(this);
	}
	
	@Override
	public TelemetryEventRepository createTelemetryEventRepository() {
		return new TelemetryEventRepositoryElasticImpl(this.elasticClient, this.etmConfiguration);
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
			.get();
	}
	
	private String createEventMapping(String type) {
		// TODO moet dit misschien met een path_match i.p.v. een match? 
		return "{\"dynamic_templates\": ["
				+ "{ \"" + this.eventTags.getContentTag() + "\": { \"match\": \"" + this.eventTags.getContentTag() + "\", \"mapping\": {\"index\": \"analyzed\"}}}"
				+ ", { \"" + this.eventTags.getCreationTimeTag() + "\": { \"match\": \"" + this.eventTags.getCreationTimeTag() + "\", \"mapping\": {\"type\": \"date\", \"index\": \"not_analyzed\"}}}"
				+ ", { \"" + this.eventTags.getExpiryTimeTag() + "\": { \"match\": \"" + this.eventTags.getExpiryTimeTag() + "\", \"mapping\": {\"type\": \"date\", \"index\": \"not_analyzed\"}}}"
				+ ", { \"other\": { \"match\": \"*\", \"mapping\": {\"index\": \"not_analyzed\"}}}"
				+ "]}";
	}
	
	@Override
	public void close() {
		this.etmConfiguration.removeConfigurationChangeListener(this);
	}

	@Override
	public void configurationChanged(ConfigurationChangedEvent event) {
		if (event.isAnyChanged(EtmConfiguration.CONFIG_KEY_REPLICAS_PER_INDEX, EtmConfiguration.CONFIG_KEY_SHARDS_PER_INDEX)) {
			createEnvironment();
		}
	}
}
