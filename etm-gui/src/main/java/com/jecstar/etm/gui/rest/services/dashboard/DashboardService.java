package com.jecstar.etm.gui.rest.services.dashboard;

import java.text.Format;
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
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation.SingleValue;
import org.elasticsearch.search.aggregations.metrics.percentiles.Percentiles;

import com.jecstar.etm.gui.rest.services.AbstractIndexMetadataService;
import com.jecstar.etm.gui.rest.services.Keyword;
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
		} else {
			throw new RuntimeException("Unknown type: '" + type + "'.");
		}
	}

	private String getNumberData(Map<String, Object> valueMap) {
		EtmPrincipal etmPrincipal = getEtmPrincipal();
		String index = getString("data_source", valueMap);
		SearchRequestBuilder searchRequest = client.prepareSearch(index)
			.setFetchSource(false)
			.setSize(0)
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		QueryStringQueryBuilder queryStringBuilder = new QueryStringQueryBuilder(getString("query", valueMap, "*"))
				.allowLeadingWildcard(true)
				.analyzeWildcard(true)
				.locale(etmPrincipal.getLocale())
				.lowercaseExpandedTerms(false)
				.timeZone(etmPrincipal.getTimeZone().getID());
		if (ElasticSearchLayout.ETM_EVENT_INDEX_ALIAS_ALL.equals(index)) {
			queryStringBuilder.defaultField("payload");
			searchRequest.setQuery(addEtmPrincipalFilterQuery(queryStringBuilder));
		} else {
			searchRequest.setQuery(queryStringBuilder);
		}
		Map<String, Object> numberData = getObject("number", valueMap);
		String aggregator = getString("aggregator", numberData);
		String field = getString("field", numberData);
		String fieldType = getString("field_type", numberData);
		StringBuilder result = new StringBuilder();
		result.append("{");
		addStringElementToJsonBuffer("type", "number", result, true);
		addStringElementToJsonBuffer("aggregator", aggregator, result, false);
		
		
		Format format = "date".equals(fieldType) ? etmPrincipal.getISO8601DateFormat() : etmPrincipal.getNumberFormat();
		String label = getString("label", numberData);
		if ("count".equals(aggregator)) {
			SearchResponse searchResponse = searchRequest.get();
			addStringElementToJsonBuffer("label", getString("label", numberData, "Count"), result, false);
			addLongElementToJsonBuffer("value", searchResponse.getHits().getTotalHits(), result, false);			
			addStringElementToJsonBuffer("value_as_string", format.format(searchResponse.getHits().getTotalHits()), result, false);			
		} else if ("median".equals(aggregator)) {
			AggregationBuilder aggregatorBuilder = createMetricAggregationBuilder(aggregator, field, label);
			SearchResponse searchResponse = searchRequest.addAggregation(aggregatorBuilder).get();
			Percentiles percentiles = searchResponse.getAggregations().get(aggregatorBuilder.getName());
			addStringElementToJsonBuffer("label", aggregatorBuilder.getName(), result, false);
			addDoubleElementToJsonBuffer("value", percentiles.percentile(50), result, false);
			addStringElementToJsonBuffer("value_as_string", Double.isNaN(percentiles.percentile(50)) ? "?" : format.format(percentiles.percentile(50)), result, false);
		} else {
			AggregationBuilder aggregatorBuilder = createMetricAggregationBuilder(aggregator, field, label);
			SearchResponse searchResponse = searchRequest.addAggregation(aggregatorBuilder).get();
			Aggregation aggregation = searchResponse.getAggregations().get(aggregatorBuilder.getName());
			NumericMetricsAggregation.SingleValue sv = (SingleValue) aggregation;
			addStringElementToJsonBuffer("label", aggregatorBuilder.getName(), result, false);
			addDoubleElementToJsonBuffer("value", sv.value(), result, false);
			addStringElementToJsonBuffer("value_as_string", Double.isNaN(sv.value()) ? "?" : format.format(sv.value()), result, false);				
		}
		result.append("}");
		return result.toString();
	}
	
	private AggregationBuilder createMetricAggregationBuilder(String type, String field, String label) {
		if ("average".equals(type)) {
			String internalLabel = label != null ? label : "Average of " + field;
			return AggregationBuilders.avg(internalLabel).field(field);
		} else if ("cardinality".equals(type)) {
			// TODO beschrijven dat dit niet een precieze waarde is: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-metrics-cardinality-aggregation.html
			String internalLabel = label != null ? label : "Unique count of " + field;
			return AggregationBuilders.cardinality(internalLabel).field(field);
		} else if ("max".equals(type)) {
			String internalLabel = label != null ? label : "Max of " + field;
			return AggregationBuilders.max(internalLabel).field(field);			
		} else if ("median".equals(type)) {
			String internalLabel = label != null ? label : "Median of " + field;
			return AggregationBuilders.percentiles(internalLabel).field(field).percentiles(50);			
		} else if ("min".equals(type)) {
			String internalLabel = label != null ? label : "Min of " + field;
			return AggregationBuilders.min(internalLabel).field(field);			
		} else if ("sum".equals(type)) {
			String internalLabel = label != null ? label : "Sum of " + field;
			return AggregationBuilders.sum(internalLabel).field(field);
		}
		throw new IllegalArgumentException("'" + type + "' is an invalid metric aggregator.");
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
