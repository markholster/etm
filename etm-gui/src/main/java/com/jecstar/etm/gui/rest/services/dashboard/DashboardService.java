package com.jecstar.etm.gui.rest.services.dashboard;

import java.text.Format;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation.SingleValue;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentileRanks;
import org.elasticsearch.search.aggregations.metrics.percentiles.Percentiles;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.jecstar.etm.gui.rest.services.AbstractIndexMetadataService;
import com.jecstar.etm.gui.rest.services.Keyword;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.AggregationValue;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.DoubleAggregationValue;
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.EtmPrincipal;

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
		if (entries != null) {
			boolean first = true;
			for (Entry<String, List<Keyword>> entry : entries) {
				if (!first) {
					result.append(", ");
				}
				first = false;
				result.append("{");
				result.append("\"index\": " + escapeToJson(indexName, true) + ",");
				result.append("\"type\": " + escapeToJson(entry.getKey(), true) + ",");
				result.append("\"keywords\": [" + entry.getValue().stream().map(n -> {
					StringBuilder kw = new StringBuilder();
					kw.append("{");
					addStringElementToJsonBuffer("name", n.getName(), kw, true);
					addStringElementToJsonBuffer("type", n.getType(), kw, false);
					addBooleanElementToJsonBuffer("date", n.isDate(), kw, false);
					addBooleanElementToJsonBuffer("number", n.isNumber(), kw, false);
					kw.append("}");
					return kw.toString();
				}).collect(Collectors.joining(", ")) + "]");
				result.append("}");
			}
		}
		result.append("]}");
		return result.toString();
	}
	
	@GET
	@Path("/graphs")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getParsers() {
		EtmPrincipal etmPrincipal = getEtmPrincipal();
		GetResponse getResponse = client.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_GRAPH, etmPrincipal.getId())
			.setFetchSource(true)
			.get();
		if (!getResponse.isExists()) {
			return null;
		}
		return getResponse.getSourceAsString();
	}
	
	@POST
	@Path("/graphdata")
	@Produces(MediaType.APPLICATION_JSON)	
	public String getGraphData(String json) {
		Map<String, Object> valueMap = toMap(json);
		String type = getString("type", valueMap);
		if ("number".equals(type)) {
			return getNumberData(valueMap);
		} else if ("bar".equals(type)) {
			return getBarOrLineData(type, valueMap);
		} else if ("line".equals(type)) {
			return getBarOrLineData(type, valueMap);
		} else {
			throw new RuntimeException("Unknown type: '" + type + "'.");
		}
	}
	
	private String getBarOrLineData(String type, Map<String, Object> valueMap) {
		EtmPrincipal etmPrincipal = getEtmPrincipal();
		BarOrLineLayout barOrLineLayout = new BarOrLineLayout();
		String index = getString("data_source", valueMap);
		String query = getString("query", valueMap, "*");
		SearchRequestBuilder searchRequest = createGraphSearchRequest(etmPrincipal, index, query);
		Map<String, Object> barOrLineData = getObject(type, valueMap);
		Map<String, Object> xAxisData = getObject("x_axis", barOrLineData);
		Map<String, Object> yAxisData = getObject("y_axis", barOrLineData);
		
		Map<String, Object> bucketAggregatorData = getObject("aggregator", xAxisData);
		BucketAggregatorWrapper bucketAggregatorWrapper = new BucketAggregatorWrapper(etmPrincipal, bucketAggregatorData);
		// First create the bucket aggregator
		AggregationBuilder bucketAggregatorBuilder = bucketAggregatorWrapper.getAggregationBuilder();
		AggregationBuilder rootForMetricAggregators = bucketAggregatorBuilder;
		// Check for the presence of a sub aggregator
		Map<String, Object> bucketSubAggregatorData = getObject("sub_aggregator", xAxisData);
		if (!bucketSubAggregatorData.isEmpty()) {
			BucketAggregatorWrapper bucketSubAggregator = new BucketAggregatorWrapper(etmPrincipal, bucketSubAggregatorData);
			rootForMetricAggregators = bucketSubAggregator.getAggregationBuilder();
			bucketAggregatorBuilder.subAggregation(rootForMetricAggregators);
		}
		
		// And add every y-axis aggregator as sub aggregator
		List<Map<String, Object>> metricsAggregatorsData = getArray("aggregators", yAxisData);
		for (Map<String, Object> metricsAggregatorData : metricsAggregatorsData) {
			String metricAggregator = getString("aggregator", metricsAggregatorData);
			String metricId = getString("id", metricsAggregatorData);
			String metricField = getString("field", metricsAggregatorData);
			String metricLabel = getString("label", metricsAggregatorData);
			Double metricPercentileDate = getDouble("percentile_data", metricsAggregatorData);
			AggregationBuilder subAggregation = createMetricAggregationBuilder(etmPrincipal, metricAggregator, metricId, metricField, metricLabel, metricPercentileDate);
			rootForMetricAggregators.subAggregation(subAggregation);
		}
		// Start building the response
		StringBuilder result = new StringBuilder();
		result.append("{");
		result.append("\"d3_formatter\": " + getD3Formatter());
		addStringElementToJsonBuffer("type", type, result, false);
		result.append(",\"data\": ");
		
		SearchResponse searchResponse = searchRequest.addAggregation(bucketAggregatorBuilder).get();
		Aggregation aggregation = searchResponse.getAggregations().get(bucketAggregatorBuilder.getName());
		if (aggregation instanceof Histogram) {
			Histogram histogram = (Histogram) aggregation;
			for(org.elasticsearch.search.aggregations.bucket.histogram.Histogram.Bucket bucket : histogram.getBuckets()) {
				String key = bucket.getKeyAsString();
				if (bucket.getKey() instanceof DateTime) {
					DateTime dateTime = (DateTime) bucket.getKey();
					key = bucketAggregatorWrapper.getBucketFormat().format(dateTime.getMillis());  
				}
				for(Aggregation subAggregation : bucket.getAggregations()) {
					AggregationValue<?> aggregationValue = getMetricAggregationValueFromAggregator(subAggregation);
					barOrLineLayout.addValueToSerie(aggregationValue.getLabel(), key, aggregationValue);
				}
			}
		} else if (aggregation instanceof Terms) {
			Terms terms = (Terms) aggregation;
			for (org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket bucket : terms.getBuckets()) {
				String key = bucket.getKeyAsString();
				if (bucket.getKey() instanceof DateTime) {
					DateTime dateTime = (DateTime) bucket.getKey();
					key = bucketAggregatorWrapper.getBucketFormat().format(dateTime.getMillis());  
				}
				for(Aggregation subAggregation : bucket.getAggregations()) {
					AggregationValue<?> aggregationValue = getMetricAggregationValueFromAggregator(subAggregation);
					barOrLineLayout.addValueToSerie(aggregationValue.getLabel(), key, aggregationValue);
				}				
			}
		} else {
			// TODO gooi exceptie
		}
		barOrLineLayout.appendAsArrayToJsonBuffer(this, result, true);
		result.append("}");
		return result.toString();
	}

	private String getNumberData(Map<String, Object> valueMap) {
		EtmPrincipal etmPrincipal = getEtmPrincipal();
		String index = getString("data_source", valueMap);
		String query = getString("query", valueMap, "*");
		SearchRequestBuilder searchRequest = createGraphSearchRequest(etmPrincipal, index, query);

		Map<String, Object> numberData = getObject("number", valueMap);
		String aggregator = getString("aggregator", numberData);
		String field = getString("field", numberData);
		String fieldType = getString("field_type", numberData);
		String label = getString("label", numberData);
		String id = getString("id", numberData);
		Double percentileDate = getDouble("percentile_data", numberData);
		
		StringBuilder result = new StringBuilder();
		result.append("{");
		addStringElementToJsonBuffer("type", "number", result, true);
		addStringElementToJsonBuffer("aggregator", aggregator, result, false);
		
		Format format = "date".equals(fieldType) ? etmPrincipal.getDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS") : etmPrincipal.getNumberFormat();

		AggregationBuilder aggregatorBuilder = createMetricAggregationBuilder(etmPrincipal, aggregator, id, field, label, percentileDate);
		SearchResponse searchResponse = searchRequest.addAggregation(aggregatorBuilder).get();
		Aggregation aggregation = searchResponse.getAggregations().get(aggregatorBuilder.getName());
		AggregationValue<?> aggregationValue = getMetricAggregationValueFromAggregator(aggregation);
		aggregationValue.appendToJsonBuffer(this, format, result, false);
		
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
		if (ElasticSearchLayout.ETM_EVENT_INDEX_ALIAS_ALL.equals(index)) {
			queryStringBuilder.defaultField("payload");
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
	
	private AggregationBuilder createMetricAggregationBuilder(EtmPrincipal etmPrincipal, String type, String id, String field, String label, Double percentileData) {
		Map<String, Object> metadata = new HashMap<>();
		AggregationBuilder builder = null;
		if ("average".equals(type)) {
			metadata.put("label", label != null ? label : "Average of " + field);
			builder = AggregationBuilders.avg(id).field(field);
		} else if ("cardinality".equals(type)) {
			// TODO beschrijven dat dit niet een precieze waarde is: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-metrics-cardinality-aggregation.html
			metadata.put("label", label != null ? label : "Unique count of " + field);
			builder = AggregationBuilders.cardinality(id).field(field);
		} else if ("count".equals(type)) {
			metadata.put("label", label != null ? label : "Count");
			builder = AggregationBuilders.count(id).field("_type");
		} else if ("max".equals(type)) {
			metadata.put("label", label != null ? label : "Max of " + field);
			builder = AggregationBuilders.max(id).field(field);			
		} else if ("median".equals(type)) {
			metadata.put("label", label != null ? label : "Median of " + field);
			builder = AggregationBuilders.percentiles(id).field(field).percentiles(50);			
		} else if ("min".equals(type)) {
			metadata.put("label", label != null ? label : "Min of " + field);
			builder = AggregationBuilders.min(id).field(field);			
		} else if ("percentile".equals(type)) {
			String internalLabel = label;
			if (internalLabel == null) {
				if (percentileData == 1) {
					internalLabel = "1st percentile of " + field;
				} else if (percentileData == 2) {
					internalLabel = "2nd percentile of " + field;
				} else if (percentileData == 3) {
					internalLabel = "3rd percentile of " + field;
				} else {
					internalLabel = etmPrincipal.getNumberFormat().format(percentileData) + "th percentile of " + field;
				}
			}
			metadata.put("label", internalLabel);
			builder = AggregationBuilders.percentiles(id).field(field).percentiles(percentileData);			
		} else if ("percentile_rank".equals(type)) {
			metadata.put("label", label != null ? label : "Percentile rank " + etmPrincipal.getNumberFormat().format(percentileData) + " of " + field);
			builder = AggregationBuilders.percentileRanks(id).field(field).values(percentileData);
		} else if ("sum".equals(type)) {
			metadata.put("label", label != null ? label : "Sum of " + field);
			builder = AggregationBuilders.sum(id).field(field);
		}
		if (builder == null) {
			throw new IllegalArgumentException("'" + type + "' is an invalid metric aggregator.");
		}
		builder.setMetaData(metadata);
		return builder;
	}
	
//	@POST
//	@Path("/chart")
//	@Produces(MediaType.APPLICATION_JSON)
//	public String getChartData(String json) {
//		Map<String, Object> valueMap = toMap(json);
//		String type = getString("type", valueMap);
//		if ("line".equals(type)) {
//			return getDataForLineChart(valueMap);
//		}
//		return null;
//	}
//
//
//	private String getDataForLineChart(Map<String, Object> valueMap) {
//		Map<String, Object> indexValues = getObject("index", valueMap);
//		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(getString("name", indexValues))
//			.setQuery(addEtmPrincipalFilterQuery(QueryBuilders.matchAllQuery()))
//			.setSize(0)
//			.setFetchSource(false)
//			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
//		List<String> types = getArray("types", indexValues);
//		if (types != null && types.size() > 0) {
//			searchRequestBuilder.setTypes(types.toArray(new String[types.size()]));
//		}
//		
//		
//		Map<String, Object> xAxisValues = getObject("x_axis", valueMap);
//		Map<String, Object> yAxisValues = getObject("y_axis", valueMap);
//		
//		Map<String, Object> xAxisAggregationValues = getObject("agg", xAxisValues);
//		Map<String, Object> yAxisAggregationValues = getObject("agg", yAxisValues);
//
//		AggregationBuilder yAxisAggregationBuilder = createAggregations(yAxisAggregationValues, false);
//		
//		AggregationBuilder xAxisAggregationBuilder = createAggregations(xAxisAggregationValues, true, yAxisAggregationBuilder);
//		searchRequestBuilder.addAggregation(xAxisAggregationBuilder);
//		
//		SearchResponse searchResponse = searchRequestBuilder.get();
//		
//		
//		InternalAggregation aggregation = searchResponse.getAggregations().get(getString("name", xAxisAggregationValues));
//		try {
//            XContentBuilder builder = XContentFactory.jsonBuilder();
//            builder.startObject();93
//            aggregation.toXContent(builder, ToXContent.EMPTY_PARAMS);
//            builder.endObject();
//            return builder.string();
//        } catch (IOException e) {
//        	throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
//        }		
//	}
//
//	private AggregationBuilder createAggregations(Map<String, Object> aggregationValues, boolean addSubAggregations, AggregationBuilder appendingSubAggregation) {
//		String type = getString("type", aggregationValues);
//		if ("count".equals(type)) {
//			return null;
//		}
//		AggregationBuilder aggregationBuilder = null;
//		if ("date-histogram".equals(type)) {
//			aggregationBuilder =  AggregationBuilders.dateHistogram(getString("name",aggregationValues))
//				.timeZone(DateTimeZone.forID(getEtmPrincipal().getTimeZone().getID()))
//				.field(getString("field", aggregationValues))
//				.dateHistogramInterval(getDateHistogramInterval(getString("interval", aggregationValues)));
//		} else if ("terms".equals(type)) {
//			aggregationBuilder = AggregationBuilders.terms(getString("name",aggregationValues))
//				.field(getString("field", aggregationValues))
//				.size(getInteger("size", aggregationValues, 10));
//		} else if ("stats".equals(type)) {
//			aggregationBuilder = AggregationBuilders.stats(getString("name",aggregationValues))
//				.field(getString("field", aggregationValues));
//// Matrix aggregations			
//		} else if ("avg".equals(type)) {
//			aggregationBuilder = AggregationBuilders.avg(getString("name",aggregationValues))
//				.field(getString("field", aggregationValues));
//		} else if ("min".equals(type)) {
//			aggregationBuilder = AggregationBuilders.min(getString("name",aggregationValues))
//					.field(getString("field", aggregationValues));
//		} else if ("max".equals(type)) {
//			aggregationBuilder = AggregationBuilders.max(getString("name",aggregationValues))
//					.field(getString("field", aggregationValues));
////		} else if ("percentiles".equals(type)) {
////			aggregationBuilder = AggregationBuilders.percentiles(getString("name",aggregationValues))
////					.field(getString("field", aggregationValues));			
//		}
//		if (aggregationBuilder != null) {
//			if (addSubAggregations) {
//				List<Map<String, Object>> subAggregations = getArray("aggs", aggregationValues);
//				if (subAggregations != null) {
//					for (Map<String, Object> subAggregation : subAggregations) {
//						AggregationBuilder subAggregationBuilder = createAggregations(subAggregation, addSubAggregations, appendingSubAggregation);
//						if (subAggregationBuilder != null) {
//							aggregationBuilder.subAggregation(subAggregationBuilder);
//						} 
//					}
//				} else if (appendingSubAggregation != null) {
//					aggregationBuilder.subAggregation(appendingSubAggregation);
//				}
//			} else if (appendingSubAggregation != null) {
//				aggregationBuilder.subAggregation(appendingSubAggregation);
//			}
//		}
//		return aggregationBuilder;		
//	}
//	
//	private AggregationBuilder createAggregations(Map<String, Object> aggregationValues, boolean addSubAggregations) {
//		return createAggregations(aggregationValues, addSubAggregations, null);
//	}
//
//
//	private DateHistogramInterval getDateHistogramInterval(String interval) {
//		if ("day".equals(interval)) {
//			return DateHistogramInterval.DAY;
//		} else if ("hour".equals(interval)) {
//			return DateHistogramInterval.HOUR;
//		} else if ("minute".equals(interval)) {
//			return DateHistogramInterval.MINUTE;
//		} else if ("month".equals(interval)) {
//			return DateHistogramInterval.MONTH;
//		} else if ("quarter".equals(interval)) {
//			return DateHistogramInterval.QUARTER;
//		} else if ("second".equals(interval)) {
//			return DateHistogramInterval.SECOND;
//		} else if ("week".equals(interval)) {
//			return DateHistogramInterval.WEEK;
//		} else if ("year".equals(interval)) {
//			return DateHistogramInterval.YEAR;
//		}
//		return null;
//	}
}
