package com.jecstar.etm.processor.repository.elasticsearch;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService.ScriptType;
import org.elasticsearch.script.groovy.GroovyScriptEngineService;

import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.TelemetryEventType;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.converter.TelemetryEventConverter;
import com.jecstar.etm.core.converter.TelemetryEventConverterTags;
import com.jecstar.etm.core.converter.json.AbstractJsonConverter;
import com.jecstar.etm.core.converter.json.TelemetryEventConverterJsonImpl;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.core.parsers.ExpressionParserFactory;
import com.jecstar.etm.processor.repository.EndpointConfigResult;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;

public class TelemetryEventRepositoryElasticImpl extends AbstractJsonConverter implements TelemetryEventRepository {
	
	private static final LogWrapper log = LogFactory.getLogger(TelemetryEventRepositoryElasticImpl.class);
	
	private static final int MAX_MODIFICATIONS_BEFORE_BLACKLISTING = 100;
	private static final int BLACKLIST_TIME = 60 * 60 * 1000; // Blacklist potential dangerous events for an hour.
	
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
	
	private Map<String, Long> blacklistedIds = new HashMap<>();
	private final Map<String, EndpointConfigResult> endpointCache = new LruCache<>(100);
	
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
				.consistencyLevel(WriteConsistencyLevel.valueOf(this.etmConfiguration.getWriteConsistency().name()))
		        .source(this.eventConverter.convert(event));
		UpdateRequest updateRequest = new UpdateRequest(index, this.eventIndexType, event.id)
				.script(new Script("etm_update-event", ScriptType.FILE, GroovyScriptEngineService.NAME,  this.updateScriptBuilder.createUpdateParameterMap(event, this.tags)))
		        .upsert(indexRequest)
		        .detectNoop(true)
		        .consistencyLevel(WriteConsistencyLevel.valueOf(this.etmConfiguration.getWriteConsistency().name()))
		        .retryOnConflict(5);
		this.bulkRequest.add(updateRequest);
		
		if (TelemetryEventType.MESSAGE_RESPONSE.equals(event.type) && event.correlationId != null) {
			indexRequest = new IndexRequest(index, this.eventIndexType, event.correlationId)
					.consistencyLevel(WriteConsistencyLevel.valueOf(this.etmConfiguration.getWriteConsistency().name()))
			        .source("{ \"response_handling_time\": " + event.creationTime.getTime() + ", \"child_correlation_ids\": [\"" + escapeToJson(event.id) + "\"] }");
			updateRequest = new UpdateRequest(index, this.eventIndexType, event.correlationId)
			        .script(new Script("etm_update-request-with-responsedata", ScriptType.FILE, GroovyScriptEngineService.NAME, this.updateScriptBuilder.createRequestUpdateScriptFromResponse(event, this.tags)))
			        .upsert(indexRequest)
			        .detectNoop(true)
			        .consistencyLevel(WriteConsistencyLevel.valueOf(this.etmConfiguration.getWriteConsistency().name()))
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
	
	@SuppressWarnings("rawtypes")
	private void executeBulk() {
		if (this.bulkRequest != null && this.bulkRequest.numberOfActions() > 0) {
			clealupBlacklist();
			Iterator<ActionRequest> it = this.bulkRequest.request().requests().iterator();
			while (it.hasNext()) {
				ActionRequest<?> action = it.next();
				if (isBlacklisted(action)) {
					it.remove();
				}
			}			
			BulkResponse bulkResponse = this.bulkRequest.get();
			for (BulkItemResponse itemResponse : bulkResponse.getItems()) {
				if (itemResponse.getVersion() > MAX_MODIFICATIONS_BEFORE_BLACKLISTING) {
					addToBlacklist(itemResponse.getId());
				}
			}			
			if (bulkResponse.hasFailures()) {
				if (log.isErrorLevelEnabled()) {
					log.logErrorMessage(buildFailureMessage(bulkResponse));
				}
			}
		}
		this.bulkRequest = null;
	}
	
	private String buildFailureMessage(BulkResponse bulkResponse) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("failure in bulk execution:");
        BulkItemResponse[] responses = bulkResponse.getItems();
        for (int i = 0; i < responses.length; i++) {
            BulkItemResponse response = responses[i];
            if (response.isFailed()) {
                buffer.append("\n[").append(i)
                        .append("]: index [").append(response.getIndex()).append("], type [").append(response.getType()).append("], id [").append(response.getId())
                        .append("], message [").append(getFailureMessage(response)).append("]");
                if (log.isDebugLevelEnabled() && response.getFailure() != null) {
                	log.logDebugMessage("Error persisting event with id '" + response.getId() + "'.", response.getFailure().getCause());
                }
             }
        }
        return buffer.toString();		
	}
	
	private String getFailureMessage(BulkItemResponse response) {
		if (response.getFailure() == null) {
			return null;
		}
		Throwable cause = response.getFailure().getCause();
		if (cause != null) {
			List<Throwable> causes = new ArrayList<Throwable>();
			Throwable rootCause = getRootCause(cause, causes);
			return rootCause.getMessage();
		}
		return response.getFailureMessage();
	}
	

	private Throwable getRootCause(Throwable cause, List<Throwable> causes) {
		if (causes.contains(cause)) {
			return causes.get(causes.size() - 1);
		}
		causes.add(cause);
		Throwable parent = cause.getCause();
		if (parent == null) {
			return cause;
		}
		return getRootCause(parent, causes);
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

	private boolean isBlacklisted(ActionRequest<?> action) {
        if (action instanceof IndexRequest) {
        	return this.blacklistedIds.containsKey(((IndexRequest) action).id());
        }  else if (action instanceof UpdateRequest) {
        	return this.blacklistedIds.containsKey(((UpdateRequest) action).id());
        } else if (action instanceof DeleteRequest) {
        	return this.blacklistedIds.containsKey(((DeleteRequest) action).id());
        } else {
            throw new IllegalArgumentException("No support for request [" + action + "]");
        }
	}

	
	private void addToBlacklist(String id) {
		if (!this.blacklistedIds.containsKey(id)) {
			this.blacklistedIds.put(id, System.currentTimeMillis());
			if (log.isInfoLevelEnabled()) {
				log.logInfoMessage("Event id '" + id + "' blacklisted for " + (BLACKLIST_TIME / 1000) + " seconds.");
			}
		}
	}
	
	private void clealupBlacklist() {
		final long startTime = System.currentTimeMillis();
		Iterator<Entry<String, Long>> iterator = this.blacklistedIds.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Long> entry = iterator.next();
			if (startTime > (entry.getValue() + BLACKLIST_TIME)) {
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Event id '" + entry.getKey() + "' no longer blacklisted.");
				}				
				iterator.remove();
			}
		}
	}
}
