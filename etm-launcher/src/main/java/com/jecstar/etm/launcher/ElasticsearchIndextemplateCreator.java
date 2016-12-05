package com.jecstar.etm.launcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import org.elasticsearch.action.admin.cluster.storedscripts.PutStoredScriptAction;
import org.elasticsearch.action.admin.cluster.storedscripts.PutStoredScriptRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsAction;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequestBuilder;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesAction;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesRequestBuilder;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateAction;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasOrIndex;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.indices.IndexTemplateAlreadyExistsException;

import com.jecstar.etm.domain.writers.TelemetryEventTags;
import com.jecstar.etm.domain.writers.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.server.core.configuration.ConfigurationChangeListener;
import com.jecstar.etm.server.core.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.EtmPrincipalRole;
import com.jecstar.etm.server.core.domain.converter.EtmConfigurationConverter;
import com.jecstar.etm.server.core.domain.converter.EtmPrincipalConverter;
import com.jecstar.etm.server.core.domain.converter.json.EtmConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.util.BCrypt;

public class ElasticsearchIndextemplateCreator implements ConfigurationChangeListener {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ElasticsearchIndextemplateCreator.class);
	
	private final TelemetryEventTags eventTags = new TelemetryEventTagsJsonImpl();
	private final MetricConverterTags metricTags = new MetricConverterTagsJsonImpl();
	private final EtmConfigurationConverter<String> etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
	private final EtmPrincipalConverter<String> etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
	private final Client elasticClient;
	private EtmConfiguration etmConfiguration;

	public ElasticsearchIndextemplateCreator(Client elasticClient) {
		this.elasticClient = elasticClient;
	}
	
	public void createTemplates() {
		try {
			GetIndexTemplatesResponse response = new GetIndexTemplatesRequestBuilder(this.elasticClient, GetIndexTemplatesAction.INSTANCE, ElasticSearchLayout.ETM_EVENT_TEMPLATE_NAME).get();
			if (response.getIndexTemplates() == null || response.getIndexTemplates().isEmpty()) {
				creatEtmEventIndexTemplate(true, 5, 0);
				insertPainlessScripts();
			}
		} catch (IndexTemplateAlreadyExistsException e) {
		} catch (IOException e) {
			if (log.isFatalLevelEnabled()) {
				log.logFatalMessage("Failed to create painless scripts. Cluster will not be usable.", e);
			}
		}
		
		try {
			GetIndexTemplatesResponse response = new GetIndexTemplatesRequestBuilder(this.elasticClient, GetIndexTemplatesAction.INSTANCE, ElasticSearchLayout.ETM_METRICS_TEMPLATE_NAME).get();
			if (response.getIndexTemplates() == null || response.getIndexTemplates().isEmpty()) {
				new PutIndexTemplateRequestBuilder(this.elasticClient, PutIndexTemplateAction.INSTANCE, ElasticSearchLayout.ETM_METRICS_TEMPLATE_NAME)
					.setCreate(false)
					.setTemplate(ElasticSearchLayout.ETM_METRICS_INDEX_PREFIX + "*")
					.setSettings(Settings.builder()
						.put("number_of_shards", 2)
						.put("number_of_replicas", 0))
					.addMapping("_default_", createMetricsMapping("_default_"))
					.addAlias(new Alias(ElasticSearchLayout.ETM_METRICS_INDEX_ALIAS_ALL))
					.get();
			}
		} catch (IndexTemplateAlreadyExistsException e) {}
		
		try {
			GetIndexTemplatesResponse response = new GetIndexTemplatesRequestBuilder(this.elasticClient, GetIndexTemplatesAction.INSTANCE, ElasticSearchLayout.CONFIGURATION_INDEX_NAME).get();
			if (response.getIndexTemplates() == null || response.getIndexTemplates().isEmpty()) {
				new PutIndexTemplateRequestBuilder(this.elasticClient, PutIndexTemplateAction.INSTANCE, ElasticSearchLayout.CONFIGURATION_INDEX_NAME)
					.setCreate(true)
					.setTemplate(ElasticSearchLayout.CONFIGURATION_INDEX_NAME)
					.setSettings(Settings.builder()
						.put("number_of_shards", 1)
						.put("number_of_replicas", 0))
					.addMapping("_default_", createEtmConfigurationMapping("_default_"))
					.get();
				insertDefaultEtmConfiguration(elasticClient);
				insertAdminUser(elasticClient);
			}
		} catch (IndexTemplateAlreadyExistsException e) {}
	}
	
	private void insertPainlessScripts() throws IOException {
		new PutStoredScriptRequestBuilder(this.elasticClient, PutStoredScriptAction.INSTANCE).setScriptLang("painless")
			.setId("etm_update-search-template").setSource(JsonXContent.contentBuilder().startObject()
					.field("script", createUpdateSearchTemplateScript()).endObject().bytes())
			.get();
		new PutStoredScriptRequestBuilder(this.elasticClient, PutStoredScriptAction.INSTANCE).setScriptLang("painless")
			.setId("etm_remove-search-template").setSource(JsonXContent.contentBuilder().startObject()
					.field("script", createRemoveSearchTemplateScript()).endObject().bytes())
			.get();
		new PutStoredScriptRequestBuilder(this.elasticClient, PutStoredScriptAction.INSTANCE).setScriptLang("painless")
			.setId("etm_update-search-history").setSource(JsonXContent.contentBuilder().startObject()
					.field("script", createUpdateSearchHistoryScript()).endObject().bytes())
			.get();
		new PutStoredScriptRequestBuilder(this.elasticClient, PutStoredScriptAction.INSTANCE).setScriptLang("painless")
			.setId("etm_update-event").setSource(JsonXContent.contentBuilder().startObject()
					.field("script", createUpdateEventScript()).endObject().bytes())
			.get();
		new PutStoredScriptRequestBuilder(this.elasticClient, PutStoredScriptAction.INSTANCE).setScriptLang("painless")
			.setId("etm_update-event-with-correlation").setSource(JsonXContent.contentBuilder().startObject()
					.field("script", createUpdateEventWithCorrelationScript()).endObject().bytes())
			.get();
		new PutStoredScriptRequestBuilder(this.elasticClient, PutStoredScriptAction.INSTANCE).setScriptLang("painless")
			.setId("etm_update-request-with-response").setSource(JsonXContent.contentBuilder().startObject()
					.field("script", createUpdateRequestWithResponseScript()).endObject().bytes())
			.get();
	}

	private void creatEtmEventIndexTemplate(boolean create, int shardsPerIndex, int replicasPerIndex) {
		new PutIndexTemplateRequestBuilder(this.elasticClient, PutIndexTemplateAction.INSTANCE, ElasticSearchLayout.ETM_EVENT_TEMPLATE_NAME)
		.setCreate(create)
		.setTemplate(ElasticSearchLayout.ETM_EVENT_INDEX_PREFIX + "*")
		.setSettings(Settings.builder()
			.put("index.number_of_shards", shardsPerIndex)
			.put("index.number_of_replicas", replicasPerIndex)
			.put("index.translog.durability", "async"))
		.addMapping("_default_", createEventMapping("_default_"))
		.addAlias(new Alias(ElasticSearchLayout.ETM_EVENT_INDEX_ALIAS_ALL))
		.get();
	}

	private String createEventMapping(String name) {
		return "{ \"" + name + "\": " 
				+ "{\"dynamic_templates\": ["
				+ "{ \"" + this.eventTags.getPayloadTag() + "\": { \"match\": \"" + this.eventTags.getPayloadTag() + "\", \"mapping\": {\"index\": \"analyzed\"}}}"
				+ ", { \"" + this.eventTags.getEndpointHandlerLocationTag() + "\": { \"match\": \"" + this.eventTags.getEndpointHandlerLocationTag() + "\", \"mapping\": {\"type\": \"geo_point\"}}}"
				+ ", { \"" + this.eventTags.getEndpointHandlerHandlingTimeTag() + "\": { \"match\": \"" + this.eventTags.getEndpointHandlerHandlingTimeTag() + "\", \"mapping\": {\"type\": \"date\", \"index\": \"not_analyzed\"}}}"
				+ ", { \"" + this.eventTags.getExpiryTag() + "\": { \"match\": \"" + this.eventTags.getExpiryTag() + "\", \"mapping\": {\"type\": \"date\", \"index\": \"not_analyzed\"}}}"
				+ ", { \"other\": { \"match\": \"*\", \"mapping\": {\"index\": \"not_analyzed\"}}}"
				+ "]}"
				+ "}";
	}
	
	private String createMetricsMapping(String name) {
		return "{ \"" + name + "\": " 
				+ "{\"dynamic_templates\": ["
				+ "{ \"" + this.metricTags.getTimestampTag() + "\": { \"match\": \"" + this.metricTags.getTimestampTag() + "\", \"mapping\": {\"type\": \"date\", \"index\": \"not_analyzed\"}}}"
				+ ", { \"other\": { \"match\": \"*\", \"mapping\": {\"index\": \"not_analyzed\"}}}]}"
				+ "}";	
	}
	
	private String createEtmConfigurationMapping(String name) {
		return "{ \"" + name + "\": {\"dynamic_templates\": [{ \"other\": { \"match\": \"*\", \"mapping\": {\"index\": \"not_analyzed\"}}}]}}";	
	}

	private void insertDefaultEtmConfiguration(Client elasticClient) {
		elasticClient.prepareIndex(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_NODE, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_NODE_DEFAULT)
			.setWaitForActiveShards(ActiveShardCount.ALL)
			.setSource(this.etmConfigurationConverter.write(null, new EtmConfiguration("temp-for-creating-default")))
			.get();
	}
	
	private void insertAdminUser(Client elasticClient) {
		EtmPrincipal adminUser = new EtmPrincipal("admin", BCrypt.hashpw("password", BCrypt.gensalt()));
		adminUser.addRole(EtmPrincipalRole.ADMIN);
		elasticClient.prepareIndex(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, adminUser.getId())
			.setWaitForActiveShards(ActiveShardCount.ALL)
			.setSource(this.etmPrincipalConverter.writePrincipal(adminUser))
			.get();	
	}
	
	
	private String createUpdateSearchTemplateScript() {
		return  "if (params.template != null) {\n" + 
				"    if (params.ctx._source.search_templates != null) {\n" +
				"        boolean found = false;\n" +
				"        for (int i=0; i < params.ctx._source.search_templates.size(); i++) {\n" + 
				"            if (params.ctx._source.search_templates[i].name.equals(params.template.name)) {\n" + 
				"                params.ctx._source.search_templates[i] = params.template;\n" + 
				"                found = true;\n" + 
				"             }\n" +
				"        }\n" + 
				"        if (!found && params.ctx._source.search_templates.size() < params.max_templates) {\n" +
				"            params.ctx._source.search_templates.add(params.template);\n" +
				"        }\n" +
				"    } else if (params.max_templates > 0) {\n" + 
				"        params.ctx._source.search_templates = new ArrayList();\n" +
				"        params.ctx._source.search_templates.add(params.template);\n" +
				"    }\n" + 
				"}\n";
	}
	
	private String createRemoveSearchTemplateScript() {
		return  "if (params.name != null) {\n" + 
				"    if (params.ctx._source.search_templates != null) {\n" +
				"		 Iterator it = params.ctx._source.search_templates.iterator();\n" +
				"        while (it.hasNext()) {\n" +
				"            def item = it.next()\n;" +	
				"            if (item.name.equals(params.name)) {\n" +	
				"                it.remove();\n" +	
				"            }\n" +	
				"        }\n" + 	
				"    }\n" + 
				"}\n";
	}
	
	private String createUpdateSearchHistoryScript() {
		return  "if (params.query != null) {\n" + 
				"    if (params.ctx._source.search_history != null) {\n" +
				"        for (int i=0; i < params.ctx._source.search_history.size(); i++) {\n" + 
				"            if (params.ctx._source.search_history[i].query.equals(params.query.query)) {\n" +
				"                params.ctx._source.search_history.remove(i);\n" +
				"            }\n" +
				"        }\n" + 
				"        params.ctx._source.search_history.add(params.query);\n" +
				"    } else {\n" + 
				"        params.ctx._source.search_history = new ArrayList();\n" +
				"        params.ctx._source.search_history.add(params.query);\n" +
				"    }\n" + 
				"}\n" +
				"if (params.ctx._source.search_history != null && params.history_size != null) {\n" +
				"    int removeCount = params.ctx._source.search_history.size() - params.history_size;\n" +
				"    for (int i=0; i < removeCount; i++) {\n" +
				"        params.ctx._source.search_history.remove(0);\n" +
				"    }\n" +
				"}\n";		
	}
	
	private String createUpdateEventWithCorrelationScript() {
		return "String correlatingId = (String)params.get(\"correlating_id\");\n" + 
				"Map targetSource = (Map)((Map)params.get(\"ctx\")).get(\"_source\");\n" + 
				"\n" + 
				"List correlations = (List)targetSource.get(\"correlations\");\n" + 
				"if (correlations == null) {\n" + 
				"	correlations = new ArrayList();\n" + 
				"	targetSource.put(\"correlations\", correlations);\n" + 
				"}\n" + 
				"if (!correlations.contains(correlatingId)) {\n" + 
				"	correlations.add(correlatingId);\n" + 
				"}";
	}
	
	private String createUpdateEventScript() {
		return "Map findEndpointByName(List endpoints, String name) {\n" + 
				"	if (endpoints == null) {\n" + 
				"		return null;\n" + 
				"	}\n" + 
				"	for (Map endpoint : endpoints) {\n" + 
				"		String endpointName = (String)endpoint.get(\"name\");\n" + 
				"		if (endpointName != null && endpointName.equals(name)) {\n" + 
				"			return endpoint;\n" + 
				"		}\n" + 
				"	}\n" + 
				"	return null;\n" + 
				"}\n" + 
				"\n" + 
				"boolean mergeEntity(Map targetSource, Map inputSource, String entityName) {\n" + 
				"	Object sourceEntity = inputSource.get(entityName);\n" + 
				"	Object targetEntity = targetSource.get(entityName);\n" + 
				"	if (sourceEntity == null) {\n" + 
				"		return false;\n" + 
				"	}\n" + 
				"	if (sourceEntity instanceof List) {\n" + 
				"		// merge a list, adding all source items to the target list\n" + 
				"		if (targetEntity == null) {\n" + 
				"			targetEntity = sourceEntity;\n" + 
				"		} else {\n" + 
				"			((List)targetEntity).addAll((List)sourceEntity);\n" + 
				"		}\n" + 
				"	} else if (sourceEntity instanceof Map) {\n" + 
				"		// merge a map, adding all items to the map if the target map didn't contain them.\n" + 
				"		if (targetEntity == null) {\n" + 
				"			targetEntity = sourceEntity;\n" + 
				"		} else {\n" + 
				"			Map original = (Map)targetEntity;\n" + 
				"			targetEntity = new HashMap((Map)sourceEntity);\n" + 
				"			((Map)targetEntity).putAll(original);\n" + 
				"		}	\n" + 
				"	} else {\n" + 
				"		// Not a collection, add the value of the target is not null.\n" + 
				"		if (targetEntity != null) {\n" + 
				"			return false;\n" + 
				"		}\n" + 
				"		targetEntity = sourceEntity;\n" + 
				"	}\n" + 
				"	targetSource.put(entityName, targetEntity);	\n" + 
				"	return true;\n" + 
				"}\n" + 
				"\n" + 
				"boolean updateResponseTimeInReaders(List endpoints, String applicationName, long handlingTime) {\n" + 
				"	if (endpoints == null) {\n" + 
				"		return false;\n" + 
				"	}\n" + 
				"	boolean updated = false;\n" + 
				"	for (Map endpoint : endpoints) {\n" + 
				"		if (endpoint.get(\"reading_endpoint_handlers\") != null) {\n" + 
				"			List readerEndpointHandlers = (List)endpoint.get(\"reading_endpoint_handlers\");\n" + 
				"			for (Map readingEndpointHandler : readerEndpointHandlers) {\n" + 
				"				if (applicationName == null && (readingEndpointHandler.get(\"application\") == null || ((Map)readingEndpointHandler.get(\"application\")).get(\"name\") == null)) {\n" + 
				"					long responseTime = handlingTime - (long)readingEndpointHandler.get(\"handling_time\");\n" + 
				"					if (responseTime >= 0) {\n" + 
				"					    readingEndpointHandler.put(\"response_time\", responseTime);\n" + 
				"						updated = true;				\n" + 
				"					}\n" + 
				"				} else if (applicationName != null && readingEndpointHandler.get(\"application\") != null && applicationName.equals(((Map)readingEndpointHandler.get(\"application\")).get(\"name\"))) {\n" + 
				"					long responseTime = handlingTime - (long)readingEndpointHandler.get(\"handling_time\");\n" + 
				"					if (responseTime >= 0) {\n" + 
				"					    readingEndpointHandler.put(\"response_time\", responseTime);\n" + 
				"						updated = true;				\n" + 
				"					}\n" + 
				"				}\n" + 
				"			}\n" + 
				"		}\n" + 
				"	}\n" + 
				"	return updated;\n" + 
				"}\n" + 
				"\n" + 
				"boolean updateResponseTimeInWriters(List endpoints, String applicationName, long handlingTime) {\n" + 
				"	if (endpoints == null) {\n" + 
				"		return false;\n" + 
				"	}\n" + 
				"	boolean updated = false;\n" + 
				"	for (Map endpoint : endpoints) {\n" + 
				"		Map writingEndpointHandler = endpoint.get(\"writing_endpoint_handler\");\n" + 
				"		if (writingEndpointHandler != null) {\n" + 
				"			if (applicationName == null && (writingEndpointHandler.get(\"application\") == null || ((Map)writingEndpointHandler.get(\"application\")).get(\"name\") == null)) {\n" + 
				"				long responseTime = handlingTime - (long)writingEndpointHandler.get(\"handling_time\");\n" + 
				"				if (responseTime >=0) {\n" + 
				"				    writingEndpointHandler.put(\"response_time\", responseTime);\n" + 
				"					updated = true;			\n" + 
				"				}	\n" + 
				"			} else if (applicationName != null && writingEndpointHandler.get(\"application\") != null && applicationName.equals(((Map)writingEndpointHandler.get(\"application\")).get(\"name\"))) {\n" + 
				"				long responseTime = handlingTime - (long)writingEndpointHandler.get(\"handling_time\");\n" + 
				"				if (responseTime >=0) {\n" + 
				"				    writingEndpointHandler.put(\"response_time\", responseTime);\n" + 
				"					updated = true;			\n" + 
				"				}	\n" + 
				"			}\n" + 
				"		}\n" + 
				"	}\n" + 
				"	return updated;\n" + 
				"}\n" + 
				"\n" + 
				"Map inputSource = (Map)params.get(\"source\");\n" + 
				"Map targetSource = (Map)((Map)params.get(\"ctx\")).get(\"_source\");\n" + 
				"\n" + 
				"// Check if we have handled this message before. Each message has an unique hashcode. We keep track of this hashcode in the elasticsearch db. \n" + 
				"// If the provided hashcode is already stored with the document we assume a duplicate offering for storage and quit processing.   \n" + 
				"long inputHash = ((List)inputSource.get(\"event_hashes\")).get(0);\n" + 
				"List targetHashes = (List)targetSource.get(\"event_hashes\");\n" + 
				"if (targetHashes != null) {\n" + 
				"	if (targetHashes.indexOf(inputHash) != -1) {\n" + 
				"		return false;\n" + 
				"	}\n" + 
				"	targetHashes.add(inputHash);\n" + 
				"}\n" + 
				"\n" + 
				"Map tempForCorrelations = (Map)targetSource.get(\"temp_for_correlations\");\n" + 
				"boolean correlatedBeforeInserted = false;\n" + 
				"if (targetSource.get(\"payload\") == null &&\n" + 
				"    targetSource.get(\"correlations\") != null) {\n" + 
				"    // The correlation to this event is stored before the event itself is stored. Merge the entire event.\n" + 
				"    correlatedBeforeInserted = true;\n" + 
				"    List correlations = (List)targetSource.get(\"correlations\");\n" + 
				"    \n" + 
				"    targetSource = inputSource;\n" + 
				"    targetSource.put(\"correlations\", correlations);\n" + 
				"    if (tempForCorrelations != null) {\n" + 
				"	    targetSource.put(\"temp_for_correlations\", tempForCorrelations);\n" + 
				"    }\n" + 
				"    ((Map)params.get(\"ctx\")).put(\"_source\", targetSource);\n" + 
				"}\n" + 
				"\n" + 
				"// Merge several fields\n" + 
				"if (!correlatedBeforeInserted) {\n" + 
				"	for (String field : inputSource.keySet()) {\n" + 
				"		if (!\"id\".equals(field) \n" + 
				"		    && !\"endpoints\".equals(field)\n" + 
				"		    && !\"event_hashes\".equals(field)\n" + 
				"		   ) {\n" + 
				"				mergeEntity(targetSource, inputSource, field);		\n" + 
				"		}\n" + 
				"	}\n" + 
				"}\n" + 
				"\n" + 
				"// Merge the endpoints.\n" + 
				"List inputEndpoints = (List)inputSource.get(\"endpoints\");\n" + 
				"List targetEndpoints = (List)targetSource.get(\"endpoints\");\n" + 
				"if (inputEndpoints != null && !correlatedBeforeInserted) {\n" + 
				"    // Merge endpoints\n" + 
				"    for (Map inputEndpoint : inputEndpoints) {\n" + 
				"        // Try to find if an endpoint with a given name is present.\n" + 
				"        Map targetEndpoint = findEndpointByName(targetEndpoints, (String)inputEndpoint.get(\"name\"));\n" + 
				"        if (targetEndpoint == null) {\n" + 
				"            // This endpoint was not present.\n" + 
				"            if (targetEndpoints == null) {\n" + 
				"            	targetEndpoints = new ArrayList();\n" + 
				"                targetSource.put(\"endpoints\", targetEndpoints);\n" + 
				"            }\n" + 
				"            targetEndpoints.add(inputEndpoint);\n" + 
				"        } else {\n" + 
				"        	Map targetWritingEndpointHandler = (Map)targetEndpoint.get(\"writing_endpoint_handler\");\n" + 
				"        	Map inputWritingEndpointHandler = (Map)inputEndpoint.get(\"writing_endpoint_handler\");\n" + 
				"            // Endpoint was present. Set writing handler to target if target has no writing handler currently.\n" + 
				"            if (inputWritingEndpointHandler != null) { \n" + 
				"            	targetEndpoint.put(\"writing_endpoint_handler\", inputWritingEndpointHandler);\n" + 
				"            }\n" + 
				"            List inputReadingEndpointHandlers = (List)inputEndpoint.get(\"reading_endpoint_handlers\"); \n" + 
				"            if (inputReadingEndpointHandlers != null) {\n" + 
				"                // Add reading endpoint handlers to target.\n" + 
				"                List targetReadingEndpointHandlers = (List)targetEndpoint.get(\"reading_endpoint_handlers\");\n" + 
				"                if (targetReadingEndpointHandlers == null) {\n" + 
				"                	targetReadingEndpointHandlers = new ArrayList();\n" + 
				"                    targetEndpoint.put(\"reading_endpoint_handlers\", targetReadingEndpointHandlers);\n" + 
				"                }\n" + 
				"                targetReadingEndpointHandlers.addAll(inputReadingEndpointHandlers);\n" + 
				"            }\n" + 
				"        }\n" + 
				"    }\n" + 
				" }\n" + 
				" \n" + 
				"// Recalculate latencies\n" + 
				"if (targetEndpoints != null) {\n" + 
				"    for (Map targetEndpont : targetEndpoints) {\n" + 
				"    	Map targetWritingEndpointHandler = (Map)targetEndpont.get(\"writing_endpoint_handler\");\n" + 
				"    	if (targetWritingEndpointHandler != null && targetWritingEndpointHandler.get(\"handling_time\") != null) {\n" + 
				"    	    long writeTime = (long)targetWritingEndpointHandler.get(\"handling_time\");\n" + 
				"    	    List readingEndpointHandlers = (List)targetEndpont.get(\"reading_endpoint_handlers\");\n" + 
				"    	    if (readingEndpointHandlers != null) {\n" + 
				"    	    	for (Map readingEndpointHandler : readingEndpointHandlers) {\n" + 
				"    	    		if (readingEndpointHandler.get(\"handling_time\") != null) {\n" + 
				"    	    			readingEndpointHandler.put(\"latency\",  ((long)readingEndpointHandler.get(\"handling_time\")) - writeTime);\n" + 
				"    	    		}\n" + 
				"    	    	}\n" + 
				"    	    }\n" + 
				"    	}\n" + 
				"    }\n" + 
				"}\n" + 
				"// Check for response times to be updated\n" + 
				"if (tempForCorrelations != null) {\n" + 
				"	List dataForReaders = (List)tempForCorrelations.get(\"data_for_readers\");\n" + 
				"	if (dataForReaders != null && targetEndpoints != null) {\n" + 
				"		Iterator it = dataForReaders.iterator();\n" + 
				"		while (it.hasNext()) {\n" + 
				"			Map dataForReader = (Map)it.next();\n" + 
				"			String appName = (String)dataForReader.get(\"name\");\n" + 
				"			long handlingTime = (long)dataForReader.get(\"handling_time\");\n" + 
				"			boolean updated = updateResponseTimeInReaders(targetEndpoints, appName, handlingTime);\n" + 
				"			if (!updated && appName == null) {\n" + 
				"				// When the appName == null also update the writers\n" + 
				"				updated = updateResponseTimeInWriters(targetEndpoints, appName, handlingTime);\n" + 
				"			}\n" + 
				"			if (updated) {\n" + 
				"				it.remove();\n" + 
				"			}\n" + 
				"		}\n" + 
				"		if (dataForReaders.isEmpty()) {\n" + 
				"			tempForCorrelations.remove(\"data_for_readers\");\n" + 
				"		}\n" + 
				"	}\n" + 
				"	List dataForWriters = (List)tempForCorrelations.get(\"data_for_writers\");\n" + 
				"	if (dataForWriters != null && targetEndpoints != null) {\n" + 
				"		Iterator it = dataForWriters.iterator();\n" + 
				"		while (it.hasNext()) {\n" + 
				"			Map dataForWriter = (Map)it.next();\n" + 
				"			String appName = (String)dataForWriter.get(\"name\");\n" + 
				"			long handlingTime = (long)dataForWriter.get(\"handling_time\");\n" + 
				"			boolean updated = updateResponseTimeInWriters(targetEndpoints, appName, handlingTime);\n" + 
				"			if (!updated && appName == null) {\n" + 
				"				// When the appName == null also update the readers\n" + 
				"				updated = updateResponseTimeInReaders(targetEndpoints, appName, handlingTime);\n" + 
				"			}\n" + 
				"			if (updated) {\n" + 
				"				it.remove();\n" + 
				"			}			\n" + 
				"		}\n" + 
				"		if (dataForWriters.isEmpty()) {\n" + 
				"			tempForCorrelations.remove(\"data_for_writers\");\n" + 
				"		}\n" + 
				"	}\n" + 
				"	\n" + 
				"	if (tempForCorrelations.isEmpty()) {\n" + 
				"		targetSource.remove(\"temp_for_correlations\");\n" + 
				"	}\n" + 
				"}";		
	}
	
	private String createUpdateRequestWithResponseScript() {
		return "boolean setResponseTimeOnReadingEndpointHandlers(List readingEndpointHandlers, String applicationName, long writerHandlingTime) {\n" + 
				"	if (readingEndpointHandlers == null) {\n" + 
				"		return false;\n" + 
				"	}\n" + 
				"	boolean readerUpdated = false;\n" + 
				"	for (Map readingEndpointHandler : readingEndpointHandlers) {\n" + 
				"		if (applicationName == null && (readingEndpointHandler.get(\"application\") == null || ((Map)readingEndpointHandler.get(\"application\")).get(\"name\") == null)) {\n" + 
				"			long responseTime = writerHandlingTime - (long)readingEndpointHandler.get(\"handling_time\");\n" + 
				"			if (responseTime >=0) {\n" + 
				"				readingEndpointHandler.put(\"response_time\", responseTime);\n" + 
				"				readerUpdated = true;\n" + 
				"			}\n" + 
				"		} else if (applicationName != null && readingEndpointHandler.get(\"application\") != null && applicationName.equals(((Map)readingEndpointHandler.get(\"application\")).get(\"name\"))) {\n" + 
				"			long responseTime = writerHandlingTime - (long)readingEndpointHandler.get(\"handling_time\");\n" + 
				"			if (responseTime >=0) {\n" + 
				"				readingEndpointHandler.put(\"response_time\", responseTime);\n" + 
				"				readerUpdated = true;\n" + 
				"			}\n" + 
				"		}\n" + 
				"	}\n" + 
				"	return readerUpdated;\n" + 
				"}\n" + 
				"\n" + 
				"boolean setResponseTimeOnWritingEndpointHandlers(List endpoints, String applicationName, long readerHandlingTime) {\n" + 
				"	if (endpoints == null) {\n" + 
				"		return false;\n" + 
				"	}\n" + 
				"	boolean writerUpdated = false;\n" + 
				"	for (Map endpoint : endpoints) {\n" + 
				"		Map writingEndpointHandler = (Map)endpoint.get(\"writing_endpoint_handler\");\n" + 
				"		if (applicationName == null && writingEndpointHandler != null && ( writingEndpointHandler.get(\"application\") == null || ((Map)writingEndpointHandler.get(\"application\")).get(\"name\") == null)) {\n" + 
				"			long responseTime = readerHandlingTime - (long)writingEndpointHandler.get(\"handling_time\");\n" + 
				"		    if (responseTime >= 0) {\n" + 
				"		    	writingEndpointHandler.put(\"response_time\", responseTime);\n" + 
				"				writerUpdated = true;\n" + 
				"			}\n" + 
				"		} else if (applicationName != null && writingEndpointHandler != null && writingEndpointHandler.get(\"application\") != null && applicationName.equals(((Map)writingEndpointHandler.get(\"application\")).get(\"name\"))) {\n" + 
				"			long responseTime = readerHandlingTime - (long)writingEndpointHandler.get(\"handling_time\");\n" + 
				"		    if (responseTime >= 0) {\n" + 
				"		    	writingEndpointHandler.put(\"response_time\", responseTime);\n" + 
				"				writerUpdated = true;\n" + 
				"			}\n" + 
				"		}\n" + 
				"	}\n" + 
				"	return writerUpdated;\n" + 
				"}\n" + 
				"\n" + 
				"void addDataForReaders(Map source, long handlingTime, String applicationName) {\n" + 
				"	Map tempCorrelation = (Map)source.get(\"temp_for_correlations\");\n" + 
				"	if (tempCorrelation == null) {\n" + 
				"		tempCorrelation = new HashMap();\n" + 
				"		source.put(\"temp_for_correlations\", tempCorrelation);\n" + 
				"	}\n" + 
				"	List dataForReaders = tempCorrelation.get(\"data_for_readers\");\n" + 
				"	if (dataForReaders == null) {\n" + 
				"		dataForReaders = new ArrayList();\n" + 
				"		tempCorrelation.put(\"data_for_readers\", dataForReaders);\n" + 
				"	}        			\n" + 
				"	Map reader = new HashMap();\n" + 
				"	if (applicationName != null) {\n" + 
				"		reader.put(\"name\", applicationName);\n" + 
				"	}\n" + 
				"	reader.put(\"handling_time\", handlingTime);\n" + 
				"	dataForReaders.add(reader);\n" + 
				"}\n" + 
				"\n" + 
				"void addDataForWriter(Map source, long handlingTime, String applicationName) {\n" + 
				"	Map tempCorrelation = (Map)source.get(\"temp_for_correlations\");\n" + 
				"	if (tempCorrelation == null) {\n" + 
				"		tempCorrelation = new HashMap();\n" + 
				"		source.put(\"temp_for_correlations\", tempCorrelation);\n" + 
				"	}\n" + 
				"	List dataForWriters = tempCorrelation.get(\"data_for_writers\");\n" + 
				"	if (dataForWriters == null) {\n" + 
				"		dataForWriters = new ArrayList();\n" + 
				"		tempCorrelation.put(\"data_for_writers\", dataForWriters);\n" + 
				"	}        			\n" + 
				"	Map writer = new HashMap();\n" + 
				"	if (applicationName != null) {\n" + 
				"		writer.put(\"name\", applicationName);\n" + 
				"	}\n" + 
				"	writer.put(\"handling_time\", handlingTime);\n" + 
				"	dataForWriters.add(writer);\n" + 
				"}\n" + 
				"\n" + 
				"Map inputSource = (Map)params.get(\"source\");\n" + 
				"Map targetSource = (Map)((Map)params.get(\"ctx\")).get(\"_source\");\n" + 
				"\n" + 
				"List correlations = (List)targetSource.get(\"correlations\");\n" + 
				"// Add the ID as a correlation.\n" + 
				"if (correlations == null) {\n" + 
				"	correlations = new ArrayList();\n" + 
				"	targetSource.put(\"correlations\", correlations);\n" + 
				"}\n" + 
				"if (!correlations.contains(inputSource.get(\"id\"))) {\n" + 
				"	correlations.add(inputSource.get(\"id\"));\n" + 
				"}\n" + 
				"// Merge the response times back in the endpoints.\n" + 
				"List inputEndpoints = (List)inputSource.get(\"endpoints\");\n" + 
				"List targetEndpoints = (List)targetSource.get(\"endpoints\");\n" + 
				"if (inputEndpoints != null) {\n" + 
				"	for (Map inputEndpoint : inputEndpoints) {\n" + 
				"		Map inputWritingEndpointHandler = (Map)inputEndpoint.get(\"writing_endpoint_handler\");\n" + 
				"        if (inputWritingEndpointHandler != null && \n" + 
				"        	(inputWritingEndpointHandler.get(\"application\") == null || ((Map)inputWritingEndpointHandler.get(\"application\")).get(\"name\") == null)) { \n" + 
				"			// This is a writer on an endpoint without an application defined. Update the response time on target readers + writers without an application defined. \n" + 
				"			long writerHandlingTime = (long)inputWritingEndpointHandler.get(\"handling_time\");\n" + 
				"        	boolean readerFound = false;\n" + 
				"        	if (targetEndpoints != null) {\n" + 
				"        		for (Map targetEndpoint : targetEndpoints) {\n" + 
				"        			readerFound = setResponseTimeOnReadingEndpointHandlers((List)targetEndpoint.get(\"reading_endpoint_handlers\"), null, writerHandlingTime) || readerFound;\n" + 
				"        		}  \n" + 
				"        	}\n" + 
				"        	boolean writerFound = false;\n" + 
				"        	if (!readerFound && targetEndpoints != null) {\n" + 
				"        		for (Map targetEndpoint : targetEndpoints) {\n" + 
				"        			Map targetWritingEndpointHandler = (Map)targetEndpoint.get(\"writing_endpoint_handler\");\n" + 
				"					if (targetWritingEndpointHandler != null && targetWritingEndpointHandler.get(\"application\") == null) {\n" + 
				"        				long responseTime = writerHandlingTime - (long)targetWritingEndpointHandler.get(\"handling_time\");\n" + 
				"        				if (responseTime >=0 ) {\n" + 
				"	        				targetWritingEndpointHandler.put(\"response_time\", responseTime);\n" + 
				"							writerFound = true;\n" + 
				"						}\n" + 
				"					}        			 \n" + 
				"        		}  \n" + 
				"        	}\n" + 
				"        	if (!readerFound && !writerFound) {\n" + 
				"        		addDataForReaders(targetSource, writerHandlingTime, null);\n" + 
				"        	}\n" + 
				"        } else if (inputWritingEndpointHandler != null && \n" + 
				"        	inputWritingEndpointHandler.get(\"application\") != null && \n" + 
				"        	((Map)inputWritingEndpointHandler.get(\"application\")).get(\"name\") != null) {\n" + 
				"        	// This is a writer on an endpoint with an application defined. Update the response time on target readers with the same application defined. \n" + 
				"        	String writerAppName = (String)((Map)inputWritingEndpointHandler.get(\"application\")).get(\"name\");\n" + 
				"        	long writerHandlingTime = (long)inputWritingEndpointHandler.get(\"handling_time\");\n" + 
				"        	// The name of the application that has written the response is found. Now try to find that application in the reading endpoint handlers of the request.\n" + 
				"        	boolean readerFound = false;\n" + 
				"        	if (targetEndpoints != null) {\n" + 
				"        		for (Map targetEndpoint : targetEndpoints) {\n" + 
				"        			readerFound = setResponseTimeOnReadingEndpointHandlers((List)targetEndpoint.get(\"reading_endpoint_handlers\"), writerAppName, writerHandlingTime) || readerFound;\n" + 
				"        		}  \n" + 
				"        	}\n" + 
				"        	if (!readerFound) {\n" + 
				"        		addDataForReaders(targetSource, writerHandlingTime, writerAppName);\n" + 
				"        	}\n" + 
				"        }\n" + 
				"        List inputReadingEndpointHandlers = (List)inputEndpoint.get(\"reading_endpoint_handlers\");\n" + 
				"        if (inputReadingEndpointHandlers != null) {\n" + 
				"        	for (Map inputReadingEndpointHandler : inputReadingEndpointHandlers) {\n" + 
				"        		if (inputReadingEndpointHandler.get(\"application\") == null || ((Map)inputReadingEndpointHandler.get(\"application\")).get(\"name\") == null) {\n" + 
				"        			// This is a reader on an endpoint without an application defined. Update the response time on target readers + writers without an application defined.\n" + 
				"		        	long readerHandlingTime = (long)inputReadingEndpointHandler.get(\"handling_time\");\n" + 
				"        			boolean writerFound = setResponseTimeOnWritingEndpointHandlers(targetEndpoints, null, readerHandlingTime);\n" + 
				"        			boolean readerFound = false;\n" + 
				"        			if (!writerFound && targetEndpoints != null) {\n" + 
				"		        		for (Map targetEndpoint : targetEndpoints) {\n" + 
				"		        			readerFound = setResponseTimeOnReadingEndpointHandlers((List)targetEndpoint.get(\"reading_endpoint_handlers\"), null, readerHandlingTime) || readerFound;\n" + 
				"		        		}  \n" + 
				"        			}\n" + 
				"        			if (!readerFound && !writerFound) {\n" + 
				"        				addDataForWriter(targetSource, readerHandlingTime, null);\n" + 
				"        			}\n" + 
				"        		} else if (inputReadingEndpointHandler.get(\"application\") != null && \n" + 
				"        			((Map)inputReadingEndpointHandler.get(\"application\")).get(\"name\") != null) {\n" + 
				"        			// This is a reader on an endpoint with an application defined. Update the response time on target writer with the same application defined. \n" + 
				"		        	String readerAppName = (String)((Map)inputReadingEndpointHandler.get(\"application\")).get(\"name\");\n" + 
				"		        	long readerHandlingTime = (long)inputReadingEndpointHandler.get(\"handling_time\");\n" + 
				"        			// The name of the application that has read the response is found. Now try to find that application in the writing endpoint handlers of the request.\n" + 
				"        			boolean writerFound = setResponseTimeOnWritingEndpointHandlers(targetEndpoints, readerAppName, readerHandlingTime);\n" + 
				"		        	if (!writerFound) {\n" + 
				"		        		addDataForWriter(targetSource, readerHandlingTime, readerAppName);\n" + 
				"		        	}\n" + 
				"        		}\n" + 
				"        	}\n" + 
				"        }\n" + 
				"	}\n" + 
				"}";
	}

	public void addConfigurationChangeNotificationListener(EtmConfiguration etmConfiguration) {
		if (this.etmConfiguration != null) {
			throw new IllegalStateException("Listener already added");
		}
		this.etmConfiguration = etmConfiguration;
		this.etmConfiguration.addConfigurationChangeListener(this);
	}
	
	public void removeConfigurationChangeNotificationListener() {
		this.etmConfiguration.removeConfigurationChangeListener(this);
	}

	@Override
	public void configurationChanged(ConfigurationChangedEvent event) {
		if (event.isAnyChanged(EtmConfiguration.CONFIG_KEY_SHARDS_PER_INDEX, EtmConfiguration.CONFIG_KEY_REPLICAS_PER_INDEX)) {
			creatEtmEventIndexTemplate(false, this.etmConfiguration.getShardsPerIndex(), this.etmConfiguration.getReplicasPerIndex());
		}
		if (event.isChanged(EtmConfiguration.CONFIG_KEY_REPLICAS_PER_INDEX)) {
			List<String> indices = new ArrayList<>();
			indices.add(ElasticSearchLayout.CONFIGURATION_INDEX_NAME);
			SortedMap<String, AliasOrIndex> aliases = this.elasticClient.admin().cluster()
				    .prepareState().execute()
				    .actionGet().getState()
				    .getMetaData().getAliasAndIndexLookup();
			if (aliases.containsKey(ElasticSearchLayout.ETM_EVENT_INDEX_ALIAS_ALL)) {
				AliasOrIndex aliasOrIndex = aliases.get(ElasticSearchLayout.ETM_EVENT_INDEX_ALIAS_ALL);
				aliasOrIndex.getIndices().forEach(c -> indices.add(c.getIndex().getName()));
			}
			if (aliases.containsKey(ElasticSearchLayout.ETM_METRICS_INDEX_ALIAS_ALL)) {
				AliasOrIndex aliasOrIndex = aliases.get(ElasticSearchLayout.ETM_METRICS_INDEX_ALIAS_ALL);
				aliasOrIndex.getIndices().forEach(c -> indices.add(c.getIndex().getName()));
			}
			new UpdateSettingsRequestBuilder(this.elasticClient, UpdateSettingsAction.INSTANCE, indices.toArray(new String[indices.size()]))
				.setSettings(Settings.builder().put("index.number_of_replicas", this.etmConfiguration.getReplicasPerIndex()))
				.get();
		}
	}

}
