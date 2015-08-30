package com.jecstar.etm.processor.elastic;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.indices.IndexTemplateAlreadyExistsException;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.processor.processor.PersistenceEnvironment;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;

public class PersistenceEnvironmentElasticImpl implements PersistenceEnvironment {

	private final EtmConfiguration etmConfiguration;
	private final Client elasticClient;

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
		new PutIndexTemplateRequestBuilder(this.elasticClient.admin().indices(), "etm")
			.setCreate(false)
			.setTemplate("etm_*")
			.setSettings(ImmutableSettings.settingsBuilder()
					/** TODO Onderstaande properties moeten in de configuratie **/
					.put("number_of_shards", 2)
					.put("number_of_replicas", 1)
					.build())
			.addMapping("_default_", createMapping("_default_"))
			.addAlias(new Alias("etm_all"))
			.addAlias(new Alias("etm_today"))
			.get();
	}
	
	private String createMapping(String type) {
		return "{" + 
				"   \"properties\": {" + 
				"	    \"id\": {" + 
				"   	    \"type\": \"string\"" + 
				"       }," + 
				"	    \"correlation_id\": {" + 
				"   	    \"type\": \"string\"" + 
				"       }," + 
				"	    \"endpoint\": {" + 
				"   	    \"type\": \"string\"" + 
				"       }," + 
				"	    \"name\": {" + 
				"   	    \"type\": \"string\"" + 
				"       }," + 
				"       \"packaging\": {" + 
				"   	    \"type\": \"string\"," + 
				"           \"index\": \"not_analyzed\"" + 
				"       }," + 
				"       \"payload\": {" + 
				"   	    \"type\": \"string\"" + 
				"       }," + 
				"       \"payload_format\": {" + 
				"   	    \"type\": \"string\"," + 
				"           \"index\": \"not_analyzed\"" + 
				"       }," + 
				"       \"reading_endpoint_handlers\": {" + 
				"   	    \"properties\": {" + 
				"       	    \"application\": {" + 
				"           	    \"properties\": {" + 
				"               	    \"instance\": {" + 
				"                   	    \"type\": \"string\"" + 
				"                       }," + 
				"                       \"name\": {" + 
				"                   	    \"type\": \"string\"" + 
				"                       }," + 
				"                       \"principal\": {" + 
				"                   	    \"type\": \"string\"" + 
				"                       }" + 
				"                   }" + 
				"               }," + 
				"               \"handling_time\": {" + 
				"           	    \"type\": \"long\"" + 
				"               }" + 
				"           }" + 
				"       }," + 
				"       \"response_time\": {" + 
				"   	    \"type\": \"long\"" + 
				"       }," + 
				"       \"transport\": {" + 
				"   	    \"type\": \"string\"," + 
				"           \"index\": \"not_analyzed\"" + 
				"       }," + 
				"       \"writing_endpoint_handler\": {" + 
				"   	    \"properties\": {" + 
				"       	    \"application\": {" + 
				"           	    \"properties\": {" + 
				"               	    \"instance\": {" + 
				"                   	    \"type\": \"string\"" + 
				"                       }," + 
				"                       \"name\": {" + 
				"                   	    \"type\": \"string\"" + 
				"                       }," + 
				"                       \"principal\": {" + 
				"                   	    \"type\": \"string\"" + 
				"                       }" + 
				"                   }" + 
				"               }," + 
				"               \"handling_time\": {" + 
				"           	    \"type\": \"long\"" + 
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
