package com.jecstar.etm.processor.repository.elasticsearch;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService.ScriptType;

import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.TelemetryEventType;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.converter.json.AbstractJsonConverter;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.core.parsers.ExpressionParserFactory;
import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.converter.TelemetryEventConverter;
import com.jecstar.etm.processor.converter.TelemetryEventConverterTags;
import com.jecstar.etm.processor.converter.json.TelemetryEventConverterJsonImpl;
import com.jecstar.etm.processor.repository.EndpointConfigResult;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;

public class TelemetryEventRepositoryElasticImpl extends AbstractJsonConverter implements TelemetryEventRepository {
	
	private final DateFormat indexDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private final String eventIndexType = "event";
	
	private final String configurationIndex = "etm_configuration";
	private final String configurationIndexTypeEndpoints = "endpoint";

	private final TelemetryEventConverter<String> eventConverter = new TelemetryEventConverterJsonImpl();
	private final TelemetryEventConverterTags tags = this.eventConverter.getTags();
	private final UpdateScriptBuilder updateScriptBuilder = new UpdateScriptBuilder();
	
	private final Client elasticClient;
	private final EtmConfiguration etmConfiguration;
	
	private BulkRequestBuilder bulkRequest;
	
	private final Map<String, EndpointConfigResult> endpointCache = new HashMap<String, EndpointConfigResult>();
	
	public TelemetryEventRepositoryElasticImpl(final Client elasticClient, final EtmConfiguration etmConfiguration) {
		this.elasticClient = elasticClient;
		this.indexDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		this.etmConfiguration = etmConfiguration;
	}

	@Override
	public void persistTelemetryEvent(TelemetryEvent event) {
		if (this.bulkRequest == null) {
			this.bulkRequest = this.elasticClient.prepareBulk();
		}
		String index = getElasticIndexName();
		
		IndexRequest indexRequest = new IndexRequest(index, this.eventIndexType, event.id)
				.consistencyLevel(WriteConsistencyLevel.QUORUM)
		        .source(this.eventConverter.convert(event));
		UpdateRequest updateRequest = new UpdateRequest(index, this.eventIndexType, event.id)
				.script(new Script("etm_update-event", ScriptType.FILE, "groovy",  this.updateScriptBuilder.createUpdateParameterMap(event, this.tags)))
		        .upsert(indexRequest)
		        .detectNoop(true)
		        .consistencyLevel(WriteConsistencyLevel.QUORUM)
		        .retryOnConflict(5);
		this.bulkRequest.add(updateRequest);
		
		if (TelemetryEventType.MESSAGE_RESPONSE.equals(event.type) && event.correlationId != null) {
			indexRequest = new IndexRequest(index, this.eventIndexType, event.correlationId)
					.consistencyLevel(WriteConsistencyLevel.QUORUM)
			        .source("{ \"response_handling_time\": " + event.creationTime.getTime() + ", \"child_correlation_ids\": [\"" + escapeToJson(event.id) + "\"] }");
			updateRequest = new UpdateRequest(index, this.eventIndexType, event.correlationId)
			        .script(new Script("etm_update-request-with-responsedata", ScriptType.FILE, "groovy", this.updateScriptBuilder.createRequestUpdateScriptFromResponse(event, this.tags)))
			        .upsert(indexRequest)
			        .detectNoop(true)
			        .consistencyLevel(WriteConsistencyLevel.QUORUM)
			        .retryOnConflict(5);
			this.bulkRequest.add(updateRequest);
		}
	
		
		if (this.bulkRequest.numberOfActions() >= this.etmConfiguration.getPersistingBulkSize()) {
			executeBulk();
		}
	}
	
	/**
	 * Gives the name of the elastic index of the given
	 * <code>TelemetryEvent</code>.
	 * 
	 * @return The name of the index.
	 */
	public String getElasticIndexName() {
		return "etm_event_" + this.indexDateFormat.format(new Date());
		
	}
	
	private void executeBulk() {
		if (this.bulkRequest != null && this.bulkRequest.numberOfActions() > 0) {
			BulkResponse bulkResponse = this.bulkRequest.get();
			if (bulkResponse.hasFailures()) {
				throw new RuntimeException(bulkResponse.buildFailureMessage());
			}
		}
		this.bulkRequest = null;
	}

	@Override
	public void findEndpointConfig(String endpoint, EndpointConfigResult result) {
		result.initialize();
		if (endpoint == null || endpoint.trim().length() == 0) {
			endpoint = "*";
		}
		EndpointConfigResult cachedResult = this.endpointCache.get(endpoint);
		if (cachedResult == null || System.currentTimeMillis() - cachedResult.retrieved > 30000) {
			if (cachedResult == null) {
				cachedResult = new EndpointConfigResult();
			}
			cachedResult.initialize();
			GetResponse getResponse = this.elasticClient.prepareGet(this.configurationIndex, this.configurationIndexTypeEndpoints, endpoint).get();
			if (getResponse.isExists()) {
				cachedResult.merge(toEndpointConfig(getResponse.getSourceAsMap()));
			}
			if (!"*".equals(endpoint)) {
				getResponse = this.elasticClient.prepareGet(this.configurationIndex, this.configurationIndexTypeEndpoints, "*").get();
				if (getResponse.isExists()) {
					cachedResult.merge(toEndpointConfig(getResponse.getSourceAsMap()));
				}
			}
			cachedResult.retrieved = System.currentTimeMillis();
			this.endpointCache.put(endpoint, cachedResult);
		} 
		result.merge(cachedResult);
	}
	
	private EndpointConfigResult toEndpointConfig(Map<String, Object> valueMap) {
		EndpointConfigResult endpointConfiguration = new EndpointConfigResult();
		endpointConfiguration.initialize();
    	String direction = getString("direction", valueMap);
    	if (direction != null) {
    		endpointConfiguration.eventDirection = TelemetryEventDirection.valueOf(direction);
    	}
    	addExpressionParsers(endpointConfiguration.applicationParsers, getStringListValue("application_parsers", valueMap));
    	addExpressionParsers(endpointConfiguration.eventNameParsers, getStringListValue("eventname_parsers", valueMap));
    	addExpressionParsers(endpointConfiguration.correlationDataParsers, getStringMapValue("correlation_parsers", valueMap));
    	addExpressionParsers(endpointConfiguration.transactionNameParsers, getStringListValue("transactionname_parsers", valueMap));
    	return endpointConfiguration;
	}
	
	@SuppressWarnings("unchecked")
	private List<String> getStringListValue(String key, Map<String, Object> valueMap) {
		if (valueMap.containsKey(key)) {
			return (List<String>) valueMap.get(key);
		}
		return Collections.emptyList();
	}
	
	private Map<String, String> getStringMapValue(String key, Map<String, Object> valueMap) {
		if (valueMap.containsKey(key)) {
			Map<String, String> result = new HashMap<String, String>();
			List<Map<String, Object>> array = getArray(key, valueMap);
			for (Map<String, Object> parse : array) {
				String resultKey = parse.keySet().iterator().next();
				result.put(resultKey, parse.get(resultKey).toString());
			}
			return result;
		}
		return Collections.emptyMap();
	}
	
	private void addExpressionParsers(List<ExpressionParser> expressionParsers, List<String> dbValues) {
		if (dbValues == null || dbValues.size() == 0) {
			return;
		}
		for (String value : dbValues) {
			expressionParsers.add(ExpressionParserFactory.createExpressionParserFromConfiguration(value));
		}
	}
	
	private void addExpressionParsers(Map<String, ExpressionParser> expressionParsers, Map<String, String> dbValues) {
		if (dbValues == null || dbValues.size() == 0) {
			return;
		}
		for (String value : dbValues.keySet()) {
			expressionParsers.put(value, ExpressionParserFactory.createExpressionParserFromConfiguration(dbValues.get(value)));
		}
	}

	@Override
	public void flushDocuments() {
		executeBulk();
	}


}
