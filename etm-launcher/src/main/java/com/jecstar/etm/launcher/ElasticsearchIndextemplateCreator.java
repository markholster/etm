package com.jecstar.etm.launcher;

import java.io.IOException;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.cluster.storedscripts.PutStoredScriptAction;
import org.elasticsearch.action.admin.cluster.storedscripts.PutStoredScriptRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesAction;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesRequestBuilder;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateAction;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.indices.IndexTemplateAlreadyExistsException;

import com.jecstar.etm.domain.writers.TelemetryEventTags;
import com.jecstar.etm.domain.writers.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.EtmPrincipal.PrincipalRole;
import com.jecstar.etm.server.core.domain.converter.EtmConfigurationConverter;
import com.jecstar.etm.server.core.domain.converter.EtmPrincipalConverter;
import com.jecstar.etm.server.core.domain.converter.json.EtmConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.util.BCrypt;

public class ElasticsearchIndextemplateCreator {
	
	private final TelemetryEventTags eventTags = new TelemetryEventTagsJsonImpl();
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
					.setSettings(Settings.builder()
						.put("number_of_shards", 5)
						.put("number_of_replicas", 0)
						.put("index.translog.durability", "async"))
					.addMapping("_default_", createEventMapping("_default_")).addAlias(new Alias("etm_event_all"))
					.get();
				new PutStoredScriptRequestBuilder(elasticClient, PutStoredScriptAction.INSTANCE)
					.setScriptLang("painless")
					.setId("etm_update-search-template")
					.setSource(JsonXContent.contentBuilder().startObject().field("script", createUpdateSearchTemplateScript()).endObject().bytes())
					.get();
				new PutStoredScriptRequestBuilder(elasticClient, PutStoredScriptAction.INSTANCE)
					.setScriptLang("painless")
					.setId("etm_remove-search-template")
					.setSource(JsonXContent.contentBuilder().startObject().field("script", createRemoveSearchTemplateScript()).endObject().bytes())
					.get();
				new PutStoredScriptRequestBuilder(elasticClient, PutStoredScriptAction.INSTANCE)
					.setScriptLang("painless")
					.setId("etm_update-query-history")
					.setSource(JsonXContent.contentBuilder().startObject().field("script", createUpdateQueryHistoryScript()).endObject().bytes())
					.get();
				new PutStoredScriptRequestBuilder(elasticClient, PutStoredScriptAction.INSTANCE)
					.setScriptLang("painless")
					.setId("etm_update-event")
					.setSource(JsonXContent.contentBuilder().startObject().field("script", createUpdateEventScript()).endObject().bytes())
					.get();
				new PutStoredScriptRequestBuilder(elasticClient, PutStoredScriptAction.INSTANCE)
					.setScriptLang("painless")
					.setId("etm_update-request-with-response")
					.setSource(JsonXContent.contentBuilder().startObject().field("script", createUpdateRequestWithResponseScript()).endObject().bytes())
					.get();
			}
		} catch (IndexTemplateAlreadyExistsException e) {
		} catch (IOException e) {
			// TODO putting templates failed.
		}
		
		try {
			GetIndexTemplatesResponse response = new GetIndexTemplatesRequestBuilder(elasticClient, GetIndexTemplatesAction.INSTANCE, "etm_metrics").get();
			if (response.getIndexTemplates() == null || response.getIndexTemplates().isEmpty()) {
				new PutIndexTemplateRequestBuilder(elasticClient, PutIndexTemplateAction.INSTANCE, "etm_metrics")
					.setCreate(true)
					.setTemplate("etm_metrics_*")
					.setSettings(Settings.builder()
						.put("number_of_shards", 1)
						.put("number_of_replicas", 0))
					.addMapping("_default_", createMetricsMapping("_default_"))
					.get();
			}
		} catch (IndexTemplateAlreadyExistsException e) {}
		
		try {
			GetIndexTemplatesResponse response = new GetIndexTemplatesRequestBuilder(elasticClient, GetIndexTemplatesAction.INSTANCE, ElasticBackedEtmConfiguration.INDEX_NAME).get();
			if (response.getIndexTemplates() == null || response.getIndexTemplates().isEmpty()) {
				new PutIndexTemplateRequestBuilder(elasticClient, PutIndexTemplateAction.INSTANCE, ElasticBackedEtmConfiguration.INDEX_NAME)
					.setCreate(true)
					.setTemplate(ElasticBackedEtmConfiguration.INDEX_NAME)
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

	private String createEventMapping(String name) {
		// TODO moet dit misschien met een path_match i.p.v. een match? 
		return "{ \"" + name + "\": " 
				+ "{\"dynamic_templates\": ["
				+ "{ \"" + this.eventTags.getPayloadTag() + "\": { \"match\": \"" + this.eventTags.getPayloadTag() + "\", \"mapping\": {\"index\": \"analyzed\"}}}"
				+ ", { \"" + this.eventTags.getEndpointHandlerLocationTag() + "\": { \"match\": \"" + this.eventTags.getEndpointHandlerLocationTag() + "\", \"mapping\": {\"type\": \"geo_point\"}}}"
				+ ", { \"" + this.eventTags.getEndpointHandlerHandlingTimeTag() + "\": { \"match\": \"" + this.eventTags.getEndpointHandlerHandlingTimeTag() + "\", \"mapping\": {\"type\": \"date\", \"index\": \"not_analyzed\"}}}"
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
		elasticClient.prepareIndex(ElasticBackedEtmConfiguration.INDEX_NAME, ElasticBackedEtmConfiguration.NODE_INDEX_TYPE, ElasticBackedEtmConfiguration.DEFAULT_ID)
			.setConsistencyLevel(WriteConsistencyLevel.ONE)
			.setSource(this.etmConfigurationConverter.write(null, new EtmConfiguration("temp-for-creating-default")))
			.get();
	}
	
	private void insertAdminUser(Client elasticClient) {
		EtmPrincipal adminUser = new EtmPrincipal("admin", BCrypt.hashpw("password", BCrypt.gensalt()));
		adminUser.addRole(PrincipalRole.ADMIN);
		elasticClient.prepareIndex(ElasticBackedEtmConfiguration.INDEX_NAME, "user", adminUser.getId())
			.setConsistencyLevel(WriteConsistencyLevel.ONE)
			.setSource(this.etmPrincipalConverter.write(adminUser))
			.get();	
	}
	
	private String createUpdateSearchTemplateScript() {
		return  "if (input.template != null) {" + 
				"    if (input.ctx._source.search_templates != null) {" +
				"        boolean found = false;" +
				"        for (int i=0; i < input.ctx._source.search_templates.size(); i++) {" + 
				"            if (input.ctx._source.search_templates[i].name.equals(input.template.name)) {" + 
				"                input.ctx._source.search_templates[i].query = input.template.query;" + 
				"                input.ctx._source.search_templates[i].types = input.template.types;" + 
				"                input.ctx._source.search_templates[i].fields = input.template.fields;" + 
				"                input.ctx._source.search_templates[i].results_per_page = input.template.results_per_page;" + 
				"                input.ctx._source.search_templates[i].sort_field = input.template.sort_field;" + 
				"                input.ctx._source.search_templates[i].sort_order = input.template.sort_order;" +
				"                found = true;" + 
				"             }" +
				"        }" + 
				"        if (!found) {" +
				"            input.ctx._source.search_templates.add(input.template);" +
				"        }" +
				"    } else {" + 
				"        input.ctx._source.search_templates = new ArrayList<Object>();" +
				"        input.ctx._source.search_templates.add(input.template);" +
				"    }" + 
				"}";
	}
	
	private String createRemoveSearchTemplateScript() {
		return  "if (input.name != null) {" + 
				"    if (input.ctx._source.search_templates != null) {" +
				"		 Iterator it = input.ctx._source.search_templates.iterator();" +
				"        while (it.hasNext()) {" +
				"            def item = it.next();" +	
				"            if (item.name.equals(input.name)) {" +	
				"                it.remove();" +	
				"            }" +	
				"        }" + 	
				"    }" + 
				"}";
	}
	
	private String createUpdateQueryHistoryScript() {
		return  "if (input.query != null) {" + 
				"    if (input.ctx._source.query_history != null) {" +
				"        for (int i=0; i < input.ctx._source.query_history.size(); i++) {" + 
				"            if (input.ctx._source.query_history[i].query.equals(input.query.query)) {" +
				"                input.ctx._source.query_history.remove(i);" +
				"            }" +
				"        }" + 
				"        input.ctx._source.query_history.add(input.query);" +
				"        int removeCount = input.ctx._source.query_history.size() - input.history_size;" +
				"        for (int i=0; i < removeCount; i++) {" +
				"            input.ctx._source.query_history.remove(0);" +
				"        }" +
				"    } else {" + 
				"        input.ctx._source.query_history = new ArrayList<Object>();" +
				"        input.ctx._source.query_history.add(input.query);" +
				"    }" + 
				"}";		
	}
	
	private String createUpdateEventScript() {
		return "// TODO merge event if only correlations is set. In that case the response is written before the request.\n" + 
				"Map inputSource = (Map)input.get(\"source\");\n" + 
				"List inputEndpoints = (List)inputSource.get(\"endpoints\");\n" + 
				"\n" + 
				"Map targetSource = (Map)((Map)input.get(\"ctx\")).get(\"_source\");\n" + 
				"List targetEndpoints = (List)targetSource.get(\"endpoints\");\n" + 
				"\n" + 
				"if (inputEndpoints != null) {\n" + 
				"    // Merge endpoints\n" + 
				"    for (int sourceEndpointIx=0; sourceEndpointIx < inputEndpoints.size(); sourceEndpointIx++) {\n" + 
				"        int targetEndpointIx = -1;\n" + 
				"        Map inputEndpoint = (Map)inputEndpoints.get(sourceEndpointIx);\n" + 
				"        // Try to find if an endpoint with a given name is present.\n" + 
				"        if (targetEndpoints != null) {\n" + 
				"            for (int i=0; i < targetEndpoints.size(); i++) { \n" + 
				"            	if ( ((String)((Map)targetEndpoints.get(i)).get(\"name\")).equals(((String)inputEndpoint.get(\"name\"))) ) {\n" + 
				"                    targetEndpointIx = i;\n" + 
				"                    break;\n" + 
				"                }\n" + 
				"            }\n" + 
				"        }\n" + 
				"        if (targetEndpointIx == -1) {\n" + 
				"            // This endpoint was not present.\n" + 
				"            if (targetEndpoints == null) {\n" + 
				"            	targetEndpoints = new ArrayList<Object>();\n" + 
				"                targetSource.put(\"endpoints\", targetEndpoints);\n" + 
				"            }\n" + 
				"            targetEndpoints.add(inputEndpoint);\n" + 
				"        } else {\n" + 
				"        	Map targetEndpoint = (Map)targetEndpoints.get(targetEndpointIx);\n" + 
				"        	Map targetWritingEndpointHandler = (Map)targetEndpoint.get(\"writing_endpoint_handler\");\n" + 
				"        	Map inputWritingEndpointHandler = (Map)inputEndpoint.get(\"writing_endpoint_handler\");\n" + 
				"            // Endpoint was present. Set writing handler to target if target has no writing handler currently.\n" + 
				"            if ((targetWritingEndpointHandler == null ||\n" + 
				"            	 targetWritingEndpointHandler.get(\"transactionId\") == null ||\n" + 
				"            	 targetWritingEndpointHandler.get(\"location\") == null ||\n" + 
				"            	 targetWritingEndpointHandler.get(\"application\") == null\n" + 
				"            	) && inputWritingEndpointHandler != null) { \n" + 
				"            	targetEndpoint.put(\"writing_endpoint_handler\", inputWritingEndpointHandler);\n" + 
				"            }\n" + 
				"            List inputReadingEndpointHandlers = (List)inputEndpoint.get(\"reading_endpoint_handlers\"); \n" + 
				"            if (inputReadingEndpointHandlers != null) {\n" + 
				"                // Add reading endpoint handlers to target.\n" + 
				"                List targetReadingEndpointHandlers = (List)targetEndpoint.get(\"reading_endpoint_handlers\");\n" + 
				"                if (targetReadingEndpointHandlers == null) {\n" + 
				"                	targetReadingEndpointHandlers = new ArrayList<Object>();\n" + 
				"                    targetEndpoint.put(\"reading_endpoint_handlers\", targetReadingEndpointHandlers);\n" + 
				"                }\n" + 
				"                for (int i=0; i < inputReadingEndpointHandlers.size(); i++) {\n" + 
				"                    targetReadingEndpointHandlers.add(inputReadingEndpointHandlers.get(i));\n" + 
				"                }\n" + 
				"            }\n" + 
				"        }\n" + 
				"    }\n" + 
				"    // Recalculate latencies\n" + 
				"    for (int i=0; i < targetEndpoints.size(); i++) {\n" + 
				"    	Map targetWritingEndpointHandler = (Map)((Map)targetEndpoints.get(i)).get(\"writing_endpoint_handler\");\n" + 
				"    	if (targetWritingEndpointHandler != null &&\n" + 
				"    	    targetWritingEndpointHandler.get(\"handling_time\") != null) {\n" + 
				"    	    long writeTime = (long)targetWritingEndpointHandler.get(\"handling_time\");\n" + 
				"    	    List readingEndpointHandlers = (List)((Map)targetEndpoints.get(i)).get(\"reading_endpoint_handlers\");\n" + 
				"    	    if (readingEndpointHandlers != null) {\n" + 
				"    	    	for (int j=0; j < readingEndpointHandlers.size(); j++) {\n" + 
				"    	    		Map readingEndpointHandler = readingEndpointHandlers.get(j);\n" + 
				"    	    		if (readingEndpointHandler.get(\"handling_time\") != null) {\n" + 
				"    	    			readingEndpointHandler.put(\"latency\",  ((long)readingEndpointHandler.get(\"handling_time\")) - writeTime);\n" + 
				"    	    		}\n" + 
				"    	    	}\n" + 
				"    	    }\n" + 
				"    	}\n" + 
				"    }\n" + 
				" }";		
	}
	
	private String createUpdateRequestWithResponseScript() {
		return "// Add the ID as a correlation.\n" + 
				"if (input.ctx._source.correlations == null) {\n" + 
				"	input.ctx._source.correlations = new ArrayList<String>();\n" + 
				"}\n" + 
				"if (!input.ctx._source.correlations.contains(input.source.id)) {\n" + 
				"	input.ctx._source.correlations.add(input.source.id);\n" + 
				"}\n" + 
				"// Merge the response times back in the endpoints.\n" + 
				"if (input.source.endpoints != null) {\n" + 
				"	for (int i=0; i < input.source.endpoints.size(); i++) {\n" + 
				"        if (input.source.endpoints[i].writing_endpoint_handler != null && \n" + 
				"        	input.source.endpoints[i].writing_endpoint_handler.application != null && \n" + 
				"        	input.source.endpoints[i].writing_endpoint_handler.application.name != null) {\n" + 
				"        	\n" + 
				"        	String writerAppName = input.source.endpoints[i].writing_endpoint_handler.application.name;\n" + 
				"        	long writerHandlingTime = input.source.endpoints[i].writing_endpoint_handler.handling_time;\n" + 
				"        	// The name of the application that has written the response is found. Now try to find that application in the reading endpoint handlers of the request.\n" + 
				"        	boolean readerFound = false;\n" + 
				"        	if (input.ctx._source.endpoints != null) {\n" + 
				"        		for (int endpointIx=0; endpointIx < input.ctx._source.endpoints.size(); endpointIx++) {\n" + 
				"        			if (input.ctx._source.endpoints[endpointIx].reading_endpoint_handlers == null) {\n" + 
				"        				continue;\n" + 
				"        			}\n" + 
				"        			for (int j=0; j < input.ctx._source.endpoints[endpointIx].reading_endpoint_handlers.size(); j++) {\n" + 
				"        				if (input.ctx._source.endpoints[endpointIx].reading_endpoint_handlers[j].application != null &&\n" + 
				"        				    input.ctx._source.endpoints[endpointIx].reading_endpoint_handlers[j].application.name != null &&\n" + 
				"        				    input.ctx._source.endpoints[endpointIx].reading_endpoint_handlers[j].application.name.equals(writerAppName)) {\n" + 
				"        				    \n" + 
				"        					readerFound = true;\n" + 
				"							input.ctx._source.endpoints[endpointIx].reading_endpoint_handlers[j].response_time = writerHandlingTime - input.ctx._source.endpoints[endpointIx].reading_endpoint_handlers[j].handling_time; \n" + 
				"        				}\n" + 
				"        			} \n" + 
				"        		}  \n" + 
				"        	}\n" + 
				"        	if (!readerFound) {\n" + 
				"        		// Write the writer in a temp field.\n" + 
				"				int TODO = 0;\n" + 
				"        	}\n" + 
				"        }\n" + 
				"        if (input.source.endpoints[i].reading_endpoint_handlers != null) {\n" + 
				"        	for (int j=0; j < input.source.endpoints[i].reading_endpoint_handlers.size(); j++) {\n" + 
				"        		if (input.source.endpoints[i].reading_endpoint_handlers[j].application != null && \n" + 
				"        			input.source.endpoints[i].reading_endpoint_handlers[j].application.name != null) {\n" + 
				"        			\n" + 
				"		        	String readerAppName = input.source.endpoints[i].reading_endpoint_handlers[j].application.name;\n" + 
				"		        	long readerHandlingTime = input.source.endpoints[i].reading_endpoint_handlers[j].handling_time;\n" + 
				"        			// The name of the application that has read the response is found. Now try to find that application in the writing endpoint handlers of the request.\n" + 
				"        			boolean writerFound = false;\n" + 
				"        			if (input.ctx._source.endpoints != null) {\n" + 
				"        				for (int endpointIx=0; endpointIx < input.ctx._source.endpoints.size(); endpointIx++) {\n" + 
				"		        			if (input.ctx._source.endpoints[endpointIx].writing_endpoint_handler != null && \n" + 
				"		        			    input.ctx._source.endpoints[endpointIx].writing_endpoint_handler.application != null &&\n" + 
				"		        			    input.ctx._source.endpoints[endpointIx].writing_endpoint_handler.application.name != null &&\n" + 
				"		        			    input.ctx._source.endpoints[endpointIx].writing_endpoint_handler.application.name.equals(readerAppName)) {\n" + 
				"		        			\n" + 
				"		        				writerFound = true;\n" + 
				"		        				input.ctx._source.endpoints[endpointIx].writing_endpoint_handler.response_time = readerHandlingTime - input.ctx._source.endpoints[endpointIx].writing_endpoint_handler.handling_time;\n" + 
				"		        			}\n" + 
				"        				}\n" + 
				"        			}\n" + 
				"		        	if (!writerFound) {\n" + 
				"		        		// Write the reader in a temp field.\n" + 
				"						int TODO = 0;\n" + 
				"		        	}\n" + 
				"        		}\n" + 
				"        	}\n" + 
				"        }\n" + 
				"	}\n" + 
				"}";
	}


}
