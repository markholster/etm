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

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverterTags;
import com.jecstar.etm.processor.processor.PersistenceEnvironment;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;

public class PersistenceEnvironmentElasticImpl implements PersistenceEnvironment {

	private final EtmConfiguration etmConfiguration;
	private final Client elasticClient;
	private final TelemetryEventConverterTags tags = new TelemetryEventConverterTagsElasticImpl();

	public PersistenceEnvironmentElasticImpl(final EtmConfiguration etmConfiguration, final Client elasticClient) {
		this.etmConfiguration = etmConfiguration;
		this.elasticClient = elasticClient;
	}
	
	@Override
	public TelemetryEventRepository createTelemetryEventRepository() {
		return new TelemetryEventRepositoryElasticImpl(this.etmConfiguration, this.elasticClient);
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
			.addMapping("_default_", createMapping("_default_"))
			.addAlias(new Alias("etm_event_all"))
			.addAlias(new Alias("etm_event_today"))
			.get();
	}
	
	private String createMapping(String type) {
		return "{" + 
				"   \"properties\": {" + 
				"	    \"" + this.tags.getCorrelationIdTag() +"\": {" + 
				"   	    \"type\": \"string\"," +
				"           \"index\": \"not_analyzed\"" + 				
				"       }," + 
				"	    \"" + this.tags.getEndpointTag() + "\": {" + 
				"   	    \"type\": \"string\"," +
				"           \"index\": \"not_analyzed\"" + 				
				"       }," + 
				"	    \"" + this.tags.getExpiryTag() + "\": {" + 
				"   	    \"type\": \"long\"," +
				"           \"index\": \"not_analyzed\"" + 				
				"       }," + 
				"	    \"" + this.tags.getNameTag() +"\": {" + 
				"   	    \"type\": \"string\"," +
				"           \"index\": \"not_analyzed\"" + 				
				"       }," + 
				"       \"" + this.tags.getPackagingTag() + "\": {" + 
				"   	    \"type\": \"string\"," + 
				"           \"index\": \"not_analyzed\"" + 
				"       }," + 
				"       \"" + this.tags.getPayloadTag() + "\": {" + 
				"   	    \"type\": \"string\"," +
				"           \"index\": \"analyzed\"" + 				
				"       }," + 
				"       \"" + this.tags.getPayloadFormatTag() + "\": {" + 
				"   	    \"type\": \"string\"," + 
				"           \"index\": \"not_analyzed\"" + 
				"       }," + 
				"       \"" + this.tags.getReadingEndpointHandlersTag() + "\": {" + 
				"   	    \"properties\": {" + 
				"       	    \"" + this.tags.getEndpointHandlerApplicationTag() + "\": {" + 
				"           	    \"properties\": {" + 
				"               	    \"" + this.tags.getApplicationInstanceTag() + "\": {" + 
				"                   	    \"type\": \"string\"," +
				"           				\"index\": \"not_analyzed\"" + 				
				"                       }," + 
				"                       \"" + this.tags.getApplicationNameTag() + "\": {" + 
				"                   	    \"type\": \"string\"," +
				"           				\"index\": \"not_analyzed\"" + 				
				"                       }," + 
				"                       \"" + this.tags.getApplicationPrincipalTag() + "\": {" + 
				"                   	    \"type\": \"string\"," +
				"           				\"index\": \"not_analyzed\"" + 				
				"                       }," + 
				"                       \"" + this.tags.getApplicationVersionTag() + "\": {" + 
				"                   	    \"type\": \"string\"," +
				"          			 		\"index\": \"not_analyzed\"" + 				
				"                       }" + 
				"                   }" + 
				"               }," + 
				"               \"" + this.tags.getEndpointHandlerHandlingTimeTag() + "\": {" + 
				"           	    \"type\": \"long\"," +
				"           		\"index\": \"not_analyzed\"" + 				
				"               }" + 
				"           }" + 
				"       }," + 
				"       \"" + this.tags.getResponseTimeTag() + "\": {" + 
				"   	    \"type\": \"long\"," +
				"           \"index\": \"not_analyzed\"" + 				
				"       }," + 
				"       \"" + this.tags.getResponseHandlingTimeTag() + "\": {" + 
				"   	    \"type\": \"long\"," +
				"           \"index\": \"not_analyzed\"" + 				
				"       }," + 
				"       \"" + this.tags.getTransactionIdTag() + "\": {" + 
				"   	    \"type\": \"string\"," + 
				"           \"index\": \"not_analyzed\"" + 
				"       }," + 
				"       \"" + this.tags.getTransportTag() + "\": {" + 
				"   	    \"type\": \"string\"," + 
				"           \"index\": \"not_analyzed\"" + 
				"       }," + 
				"       \"" + this.tags.getWritingEndpointHandlerTag() + "\": {" + 
				"   	    \"properties\": {" + 
				"       	    \"" + this.tags.getEndpointHandlerApplicationTag() + "\": {" + 
				"           	    \"properties\": {" + 
				"               	    \"" + this.tags.getApplicationInstanceTag() + "\": {" + 
				"                   	    \"type\": \"string\"," +
				"           				\"index\": \"not_analyzed\"" + 				
				"                       }," + 
				"                       \"" + this.tags.getApplicationNameTag() + "\": {" + 
				"                   	    \"type\": \"string\"," +
				"           				\"index\": \"not_analyzed\"" + 				
				"                       }," + 
				"                       \"" + this.tags.getApplicationPrincipalTag() + "\": {" + 
				"                   	    \"type\": \"string\"," +
				"           				\"index\": \"not_analyzed\"" + 				
				"                       }," + 
				"                       \"" + this.tags.getApplicationVersionTag() + "\": {" + 
				"                   	    \"type\": \"string\"," +
				"           				\"index\": \"not_analyzed\"" + 				
				"                       }" + 
				"                   }" + 
				"               }," + 
				"               \"" + this.tags.getEndpointHandlerHandlingTimeTag() + "\": {" + 
				"           	    \"type\": \"long\"," +
				"           		\"index\": \"not_analyzed\"" + 				
				"               }" + 
				"           }" + 
				"       }" + 
				"    }" + 
				"}";
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
