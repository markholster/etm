package com.jecstar.etm.gui.rest.services.dashboard;

import java.text.Format;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation.Bucket;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation.SingleValue;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentileRanks;
import org.elasticsearch.search.aggregations.metrics.percentiles.Percentiles;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.jecstar.etm.gui.rest.services.AbstractIndexMetadataService;
import com.jecstar.etm.gui.rest.services.Keyword;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.AggregationKey;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.AggregationValue;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.DateTimeAggregationKey;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.DoubleAggregationKey;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.DoubleAggregationValue;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.LongAggregationKey;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.StringAggregationKey;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;

@Path("/dashboard")
public class DashboardService extends AbstractIndexMetadataService {
	
	private static Client client;
	private static EtmConfiguration etmConfiguration;
	
	public static void initialize(Client client, EtmConfiguration etmConfiguration) {
		DashboardService.client = client;
		DashboardService.etmConfiguration = etmConfiguration;
	}
	
	@GET
	@Path("/keywords/{indexName}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getKeywords(@PathParam("indexName") String indexName) {
		StringBuilder result = new StringBuilder();
		Map<String, List<Keyword>> names = getIndexFields(DashboardService.client, indexName);
		result.append("{ \"keywords\":[");
		Set<Entry<String, List<Keyword>>> entries = names.entrySet();
		boolean first = true;
		for (Entry<String, List<Keyword>> entry : entries) {
			if (!first) {
				result.append(", ");
			}
			first = false;
			result.append("{");
			result.append("\"index\": ").append(escapeToJson(indexName, true)).append(",");
			result.append("\"type\": ").append(escapeToJson(entry.getKey(), true)).append(",");
			result.append("\"keywords\": [").append(entry.getValue().stream().map(n -> {
				StringBuilder kw = new StringBuilder();
				kw.append("{");
				addStringElementToJsonBuffer("name", n.getName(), kw, true);
				addStringElementToJsonBuffer("type", n.getType(), kw, false);
				addBooleanElementToJsonBuffer("date", n.isDate(), kw, false);
				addBooleanElementToJsonBuffer("number", n.isNumber(), kw, false);
				kw.append("}");
				return kw.toString();
			}).collect(Collectors.joining(", "))).append("]");
			result.append("}");
		}
		result.append("]}");
		return result.toString();
	}
	
	@GET
	@Path("/graphs")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getGraphs() {
		GetResponse getResponse = DashboardService.client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_INDEX_TYPE_GRAPH, getEtmPrincipal().getId())
				.setFetchSource("graphs", null)
				.get();
		if (getResponse.isSourceEmpty() || getResponse.getSourceAsMap().isEmpty()) {
			return "{\"max_graphs\": " + etmConfiguration.getMaxGraphCount() + "}";
		}
		// Hack the max search graphs into the result. Dunno how to do this better.
		StringBuilder result = new StringBuilder(getResponse.getSourceAsString().substring(0, getResponse.getSourceAsString().lastIndexOf("}")));
		addIntegerElementToJsonBuffer("max_graphs", etmConfiguration.getMaxGraphCount(), result, false);
		result.append("}");
		return result.toString();
	}
	
	@PUT
	@Path("/graph/{graphName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String addGraph(@PathParam("graphName") String graphName, String json) {
		// Execute a dry run on all data.
		getGraphData(json, true);
		// Data seems ok, now store the graph.
		GetResponse getResponse = DashboardService.client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_INDEX_TYPE_GRAPH, getEtmPrincipal().getId())
				.setFetchSource("graphs", null)
				.get();
		List<Map<String, Object>> currentGraphs = new ArrayList<>();
		Map<String, Object> valueMap = toMap(json);
		valueMap.put("name", graphName);
		if (!getResponse.isSourceEmpty()) {
			currentGraphs = getArray("graphs", getResponse.getSourceAsMap());
		}
		ListIterator<Map<String, Object>> iterator = currentGraphs.listIterator();
		boolean updated = false;
		while (iterator.hasNext()) {
			Map<String, Object> graph = iterator.next();
			if (graphName.equals(getString("name", graph))) {
				iterator.set(valueMap);
				updated = true;
				break;
			}
		}
		if (!updated) {
			if (currentGraphs.size() >= etmConfiguration.getMaxGraphCount()) {
				throw new EtmException(EtmException.MAX_NR_OF_GRAPHS_REACHED);
			}
			currentGraphs.add(valueMap);
		}
		Map<String, Object> source = new HashMap<>();
		source.put("graphs", currentGraphs);
		DashboardService.client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_INDEX_TYPE_GRAPH, getEtmPrincipal().getId())
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
			.setDoc(source)
			.setDocAsUpsert(true)
			.get();
		return "{\"status\":\"success\"}";
	}
	
	@DELETE
	@Path("/graph/{graphName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String deleteGraph(@PathParam("graphName") String graphName) {
		GetResponse getResponse = DashboardService.client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_INDEX_TYPE_GRAPH, getEtmPrincipal().getId())
				.setFetchSource("graphs", null)
				.get();
		List<Map<String, Object>> currentGraphs = new ArrayList<>();
		if (!getResponse.isSourceEmpty()) {
			currentGraphs = getArray("graphs", getResponse.getSourceAsMap());
		}
		ListIterator<Map<String, Object>> iterator = currentGraphs.listIterator();
		while (iterator.hasNext()) {
			Map<String, Object> graph = iterator.next();
			if (graphName.equals(getString("name", graph))) {
				iterator.remove();
				break;
			}
		}
		
		BulkRequestBuilder bulkRequest = DashboardService.client.prepareBulk()
				.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));

		
		Map<String, Object> source = new HashMap<>();
		source.put("graphs", currentGraphs);
		bulkRequest.add(DashboardService.client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_INDEX_TYPE_GRAPH, getEtmPrincipal().getId())
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
			.setDoc(source)
			.setDocAsUpsert(true));
		
		// Iterate over all dashboards of the user to remove the graph if it is present in the dashboard
		getResponse = DashboardService.client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_INDEX_TYPE_DASHBOARD, getEtmPrincipal().getId())
				.setFetchSource("dashboards", null)
				.get();
		if (!getResponse.isSourceEmpty() && !getResponse.getSourceAsMap().isEmpty()) {
			boolean dashboardsUpdated = false;
			Map<String, Object> sourceValues = getResponse.getSourceAsMap();
			List<Map<String, Object>> dashboardsValues = getArray("dashboards", sourceValues);
			if (dashboardsValues != null) {
				for (Map<String, Object> dashboardValues : dashboardsValues) {
					List<Map<String, Object>> rowsValues = getArray("rows", dashboardValues);
					if (rowsValues != null) {
						for (Map<String, Object> rowValues : rowsValues) {
							List<Map<String, Object>> colsValues = getArray("cols", rowValues);
							if (colsValues != null) {
								for (Map<String, Object> colValues : colsValues) {
									if (graphName.equals(colValues.get("graph"))) {
										colValues.remove("graph");
										colValues.remove("query");
										colValues.remove("title");
										colValues.remove("refresh_rate");
										dashboardsUpdated = true;
									}
								}
							}
						}
					}
				}
			}
			if (dashboardsUpdated) {
				bulkRequest.add(DashboardService.client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_INDEX_TYPE_DASHBOARD, getEtmPrincipal().getId())
					.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
					.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
					.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
					.setDoc(sourceValues)
					.setDocAsUpsert(true));
			}
		}
		bulkRequest.get();
		return "{\"status\":\"success\"}";
	}
	
	@GET
	@Path("/dashboards")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getDashboards() {
		GetResponse getResponse = DashboardService.client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_INDEX_TYPE_DASHBOARD, getEtmPrincipal().getId())
				.setFetchSource("dashboards", null)
				.get();
		if (getResponse.isSourceEmpty() || getResponse.getSourceAsMap().isEmpty()) {
			return "{\"max_dashboards\": " + etmConfiguration.getMaxDashboardCount() + "}";
		}
		// Hack the max search graphs into the result. Dunno how to do this better.
		StringBuilder result = new StringBuilder(getResponse.getSourceAsString().substring(0, getResponse.getSourceAsString().lastIndexOf("}")));
		addIntegerElementToJsonBuffer("max_dashboards", etmConfiguration.getMaxDashboardCount(), result, false);
		result.append("}");
		return result.toString();
	}
	
	@PUT
	@Path("/dashboard/{dashboardName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String addDashboard(@PathParam("dashboardName") String dasboardName, String json) {
		// Execute a dry run on all data.
		// TODO validate all data
//		getGraphData(json, true);
		// Data seems ok, now store the graph.
		GetResponse getResponse = DashboardService.client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_INDEX_TYPE_DASHBOARD, getEtmPrincipal().getId())
				.setFetchSource("dashboards", null)
				.get();
		List<Map<String, Object>> currentDashboards = new ArrayList<>();
		Map<String, Object> valueMap = toMap(json);
		valueMap.put("name", dasboardName);
		if (!getResponse.isSourceEmpty()) {
			currentDashboards = getArray("dashboards", getResponse.getSourceAsMap());
		}
		ListIterator<Map<String, Object>> iterator = currentDashboards.listIterator();
		boolean updated = false;
		while (iterator.hasNext()) {
			Map<String, Object> dashboard = iterator.next();
			if (dasboardName.equals(getString("name", dashboard))) {
				iterator.set(valueMap);
				updated = true;
				break;
			}
		}
		if (!updated) {
			if (currentDashboards.size() >= etmConfiguration.getMaxGraphCount()) {
				throw new EtmException(EtmException.MAX_NR_OF_DASHBOARDS_REACHED);
			}
			currentDashboards.add(valueMap);
		}
		Map<String, Object> source = new HashMap<>();
		source.put("dashboards", currentDashboards);
		DashboardService.client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_INDEX_TYPE_DASHBOARD, getEtmPrincipal().getId())
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
			.setDoc(source)
			.setDocAsUpsert(true)
			.get();
		return "{\"status\":\"success\"}";
	}
	
	@DELETE
	@Path("/dashboard/{dashboardName}")
	@Produces(MediaType.APPLICATION_JSON)	
	public String deleteDashboard(@PathParam("dashboardName") String dashboardName) {
		GetResponse getResponse = DashboardService.client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_INDEX_TYPE_DASHBOARD, getEtmPrincipal().getId())
				.setFetchSource("dashboards", null)
				.get();
		List<Map<String, Object>> currentDashboards = new ArrayList<>();
		if (!getResponse.isSourceEmpty()) {
			currentDashboards = getArray("dashboards", getResponse.getSourceAsMap());
		}
		ListIterator<Map<String, Object>> iterator = currentDashboards.listIterator();
		while (iterator.hasNext()) {
			Map<String, Object> dashboard = iterator.next();
			if (dashboardName.equals(getString("name", dashboard))) {
				iterator.remove();
				break;
			}
		}
		Map<String, Object> source = new HashMap<>();
		source.put("dashboards", currentDashboards);
		DashboardService.client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_INDEX_TYPE_DASHBOARD, getEtmPrincipal().getId())
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
			.setDoc(source)
			.setDocAsUpsert(true)
			.get();
		return "{\"status\":\"success\"}";
	}
	
	@POST
	@Path("/graphdata")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getGraphData(String json) {
		return getGraphData(json, false);
	}
	
	private String getGraphData(String json, boolean dryRun) {
		Map<String, Object> valueMap = toMap(json);
		String type = getString("type", valueMap);
		if ("number".equals(type)) {
			return getNumberData(valueMap, dryRun);
		} else if ("bar".equals(type)) {
			return getMultiBucketData(type, valueMap, true, dryRun);
		} else if ("line".equals(type)) {
			return getMultiBucketData(type, valueMap, true, dryRun);
		} else if ("stacked_area".equals(type)) {
			return getMultiBucketData(type, valueMap, true, dryRun);
		} else {
			throw new RuntimeException("Unknown type: '" + type + "'.");
		}		
	}
	
	private String getMultiBucketData(String type, Map<String, Object> valueMap, boolean addMissingKeys, boolean dryRun) {
		EtmPrincipal etmPrincipal = getEtmPrincipal();
		MultiBucketResult multiBucketResult = new MultiBucketResult();
		multiBucketResult.setAddMissingKeys(addMissingKeys);
		String index = getString("data_source", valueMap);
		String query = getString("query", valueMap, "*");
		SearchRequestBuilder searchRequest = createGraphSearchRequest(etmPrincipal, index, query);
		Map<String, Object> graphData = getObject(type, valueMap);
		Map<String, Object> xAxisData = getObject("x_axis", graphData);
		Map<String, Object> yAxisData = getObject("y_axis", graphData);
		
		Map<String, Object> bucketAggregatorData = getObject("aggregator", xAxisData);
		BucketAggregatorWrapper bucketAggregatorWrapper = new BucketAggregatorWrapper(etmPrincipal, bucketAggregatorData, createGraphSearchRequest(etmPrincipal, index, query));
		BucketAggregatorWrapper bucketSubAggregatorWrapper = null;
		// First create the bucket aggregator
		AggregationBuilder bucketAggregatorBuilder = bucketAggregatorWrapper.getAggregationBuilder();
		AggregationBuilder rootForMetricAggregators = bucketAggregatorBuilder;
		// Check for the presence of a sub aggregator
		Map<String, Object> bucketSubAggregatorData = getObject("sub_aggregator", xAxisData);
		if (!bucketSubAggregatorData.isEmpty()) {
			bucketSubAggregatorWrapper = new BucketAggregatorWrapper(etmPrincipal, bucketSubAggregatorData);
			rootForMetricAggregators = bucketSubAggregatorWrapper.getAggregationBuilder();
			bucketAggregatorBuilder.subAggregation(rootForMetricAggregators);
		}
		
		// And add every y-axis aggregator as sub aggregator
		List<Map<String, Object>> metricsAggregatorsData = getArray("aggregators", yAxisData);
		for (Map<String, Object> metricsAggregatorData : metricsAggregatorsData) {
			rootForMetricAggregators.subAggregation(new MetricAggregatorWrapper(etmPrincipal, metricsAggregatorData).getAggregationBuilder());
		}
		if (dryRun) {
			return null;
		}
		SearchResponse searchResponse = searchRequest.addAggregation(bucketAggregatorBuilder).get();
		Aggregation aggregation = searchResponse.getAggregations().get(bucketAggregatorBuilder.getName());

		// Start building the response
		StringBuilder result = new StringBuilder();
		result.append("{");
		result.append("\"d3_formatter\": ").append(getD3Formatter());
		addStringElementToJsonBuffer("type", type, result, false);
		result.append(",\"data\": ");
		
		if (!(aggregation instanceof MultiBucketsAggregation)) {
			throw new IllegalArgumentException("'" + aggregation.getClass().getName() + "' is an invalid multi bucket aggregation.");
		}
		MultiBucketsAggregation multiBucketsAggregation = (MultiBucketsAggregation) aggregation;
		for(Bucket bucket : multiBucketsAggregation.getBuckets()) {
			AggregationKey key = getFormattedBucketKey(bucket, bucketAggregatorWrapper.getBucketFormat());
			for(Aggregation subAggregation : bucket.getAggregations()) {
				if (subAggregation instanceof MultiBucketsAggregation) {
					// A sub aggregation on the x-axis.
					MultiBucketsAggregation subBucketsAggregation = (MultiBucketsAggregation) subAggregation;
					for(Bucket subBucket : subBucketsAggregation.getBuckets()) { 
						AggregationKey subKey = getFormattedBucketKey(subBucket, bucketSubAggregatorWrapper.getBucketFormat());
						for(Aggregation metricAggregation : subBucket.getAggregations()) {
							AggregationValue<?> aggregationValue = getMetricAggregationValueFromAggregator(metricAggregation);
							multiBucketResult.addValueToSerie(subKey.getKeyAsString() + ": " + aggregationValue.getLabel(), key, aggregationValue);								
						}
					}
				} else {
					AggregationValue<?> aggregationValue = getMetricAggregationValueFromAggregator(subAggregation);
					multiBucketResult.addValueToSerie(aggregationValue.getLabel(), key, aggregationValue);
				}
			}
		}
		multiBucketResult.appendAsArrayToJsonBuffer(this, result, true);
		result.append("}");
		return result.toString();
	}
	
	private AggregationKey getFormattedBucketKey(Bucket bucket, Format format) {
		if (bucket.getKey() instanceof DateTime) {
			DateTime dateTime = (DateTime) bucket.getKey();
			return new DateTimeAggregationKey(dateTime.getMillis(), format);
		} else if (bucket.getKey() instanceof Double) {
			return new DoubleAggregationKey((Double) bucket.getKey(), format);
		} else if (bucket.getKey() instanceof Long) {
			return new LongAggregationKey((Long) bucket.getKey(), format);
		}
		return new StringAggregationKey(bucket.getKeyAsString());
	}

	private String getNumberData(Map<String, Object> valueMap, boolean dryRun) {
		EtmPrincipal etmPrincipal = getEtmPrincipal();
		String index = getString("data_source", valueMap);
		String query = getString("query", valueMap, "*");
		SearchRequestBuilder searchRequest = createGraphSearchRequest(etmPrincipal, index, query);

		Map<String, Object> numberData = getObject("number", valueMap);
		MetricAggregatorWrapper metricAggregatorWrapper = new MetricAggregatorWrapper(etmPrincipal, numberData);
		if (dryRun) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		result.append("{");
		addStringElementToJsonBuffer("type", "number", result, true);
		addStringElementToJsonBuffer("aggregator", metricAggregatorWrapper.getAggregatorType(), result, false);
		
		AggregationBuilder aggregatorBuilder = metricAggregatorWrapper.getAggregationBuilder();
		SearchResponse searchResponse = searchRequest.addAggregation(aggregatorBuilder).get();
		Aggregation aggregation = searchResponse.getAggregations().get(aggregatorBuilder.getName());
		AggregationValue<?> aggregationValue = getMetricAggregationValueFromAggregator(aggregation);
		aggregationValue.appendToJsonBuffer(this, metricAggregatorWrapper.getFieldFormat(), result, false);
		
		result.append("}");
		return result.toString();
	}
	
	private SearchRequestBuilder createGraphSearchRequest(EtmPrincipal etmPrincipal, String index, String query) {
		SearchRequestBuilder searchRequest = client.prepareSearch(index)
				.setFetchSource(false)
				.setSize(0)
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		QueryStringQueryBuilder queryStringBuilder = new QueryStringQueryBuilder(query)
				.allowLeadingWildcard(true)
				.analyzeWildcard(true)
				.timeZone(DateTimeZone.forTimeZone(etmPrincipal.getTimeZone()));
		if (ElasticsearchLayout.ETM_EVENT_INDEX_ALIAS_ALL.equals(index)) {
			queryStringBuilder.defaultField("_all");
			searchRequest.setQuery(addEtmPrincipalFilterQuery(queryStringBuilder));
		} else {
			searchRequest.setQuery(queryStringBuilder);
		}		
		return searchRequest;
	}
	
	private AggregationValue<?> getMetricAggregationValueFromAggregator(Aggregation aggregation) {
		Map<String, Object> metadata = aggregation.getMetaData();
		if (aggregation instanceof Percentiles) {
			Percentiles percentiles = (Percentiles) aggregation;
			return new DoubleAggregationValue(metadata.get("label").toString(), percentiles.iterator().next().getValue());
		} else if (aggregation instanceof PercentileRanks) {
			PercentileRanks percentileRanks = (PercentileRanks) aggregation;
			return new DoubleAggregationValue(metadata.get("label").toString(), percentileRanks.iterator().next().getPercent()).setPercentage(true);
		} else if (aggregation instanceof NumericMetricsAggregation.SingleValue) {
			NumericMetricsAggregation.SingleValue singleValue = (SingleValue) aggregation;
			return new DoubleAggregationValue(metadata.get("label").toString(), singleValue.value());
		}
		throw new IllegalArgumentException("'" + aggregation.getClass().getName() + "' is an invalid metric aggregator.");
	}
	
}
