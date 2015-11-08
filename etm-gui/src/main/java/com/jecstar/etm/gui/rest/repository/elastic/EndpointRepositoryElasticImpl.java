package com.jecstar.etm.gui.rest.repository.elastic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;

import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.converter.json.AbstractJsonConverter;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.core.parsers.ExpressionParserFactory;
import com.jecstar.etm.gui.rest.repository.EndpointConfiguration;
import com.jecstar.etm.gui.rest.repository.EndpointRepository;

public class EndpointRepositoryElasticImpl extends AbstractJsonConverter implements EndpointRepository {

	private final String configurationIndex = "etm_configuration";
	private final String configurationIndexTypeEndpoints = "endpoint";
	private final String defaultEndpointConfigName = "*";
	private final Client elasticClient;
	

	public EndpointRepositoryElasticImpl(Client elasticClient) {
	    this.elasticClient = elasticClient;
    }
	
	public List<String> getEndpointNames() {
		final String distinctEndpointsAggregation = "distinct_endpoints";
		TermsBuilder termsBuilder = AggregationBuilders.terms(distinctEndpointsAggregation).field("name");
		
		SearchResponse searchResponse = this.elasticClient.prepareSearch(this.configurationIndex)
				.setTypes(this.configurationIndexTypeEndpoints)
				.setSearchType(SearchType.COUNT)
				.setQuery(QueryBuilders.matchAllQuery())
				.addAggregation(termsBuilder)
				.get();
		
		List<String> endpointNames = new ArrayList<String>();
		Terms terms = searchResponse.getAggregations().get(distinctEndpointsAggregation);
		List<Terms.Bucket> endpointBuckets = terms.getBuckets();
		for (Terms.Bucket transactionBucket : endpointBuckets) {
			endpointNames.add(transactionBucket.getKey());
		}
		int ix = endpointNames.indexOf(this.defaultEndpointConfigName);
		if (ix != -1) {
			endpointNames.remove(ix);
		}
		Collections.sort(endpointNames);
		return endpointNames;
	}

	public EndpointConfiguration getEndpointConfiguration(String endpointName) {
		EndpointConfiguration endpointConfiguration = new EndpointConfiguration();
		endpointConfiguration.name = endpointName;
		GetResponse getResponse = this.elasticClient.prepareGet(this.configurationIndex, this.configurationIndexTypeEndpoints, endpointName).get();
		if (getResponse.isExists()) {
			Map<String, Object> valueMap = getResponse.getSourceAsMap();
	    	String direction = getString("direction", valueMap);
	    	if (direction != null) {
	    		endpointConfiguration.direction = TelemetryEventDirection.valueOf(direction);
	    	}
	    	addExpressionParsers(endpointConfiguration.applicationParsers, getStringListValue("application_parsers", valueMap));
	    	addExpressionParsers(endpointConfiguration.eventNameParsers, getStringListValue("eventname_parsers", valueMap));
	    	addExpressionParsers(endpointConfiguration.correlationParsers, getStringMapValue("correlation_parsers", valueMap));
	    	addExpressionParsers(endpointConfiguration.transactionNameParsers, getStringListValue("transactionname_parsers", valueMap));
	    }
	    return endpointConfiguration;
    }
	
	public void deleteEndpointConfiguration(String endpointName) {
		this.elasticClient.prepareDelete(this.configurationIndex, this.configurationIndexTypeEndpoints, endpointName)
			.setConsistencyLevel(WriteConsistencyLevel.ONE)
			.get();
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

	public void updateEnpointConfiguration(EndpointConfiguration endpointConfiguration) {
		Map<String, Object> source = new HashMap<String, Object>();
		source.put("name", endpointConfiguration.name);
		if (endpointConfiguration.direction != null) {
			source.put("direction", endpointConfiguration.direction.name());
		}
		source.put("application_parsers", toExpressionConfigurationList(endpointConfiguration.applicationParsers));
		source.put("eventname_parsers", toExpressionConfigurationList(endpointConfiguration.eventNameParsers));
		source.put("correlation_parsers", toExpressionCongigurationMap(endpointConfiguration.correlationParsers));
		source.put("transactionname_parsers", toExpressionConfigurationList(endpointConfiguration.transactionNameParsers));
		this.elasticClient.prepareUpdate(this.configurationIndex, this.configurationIndexTypeEndpoints, endpointConfiguration.name)
			.setConsistencyLevel(WriteConsistencyLevel.ONE)
			.setDocAsUpsert(true)
			.setDoc(source)
			.get();
    }
	
	private List<String> toExpressionConfigurationList(List<ExpressionParser> expressionParsers) {
		return expressionParsers.stream().map(e -> ExpressionParserFactory.toConfiguration(e)).collect(Collectors.toList());
	}
	
	private List<Map<String, Object>> toExpressionCongigurationMap(Map<String, ExpressionParser> expressionParsers) {
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for (String key : expressionParsers.keySet()) {
			Map<String, Object> parser = new HashMap<String, Object>();
			parser.put(key, ExpressionParserFactory.toConfiguration(expressionParsers.get(key)));
			result.add(parser);
		}
		return result;
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

}
