package com.jecstar.etm.gui.rest.services.dashboard;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.joda.time.DateTimeZone;

import com.jecstar.etm.gui.rest.AbstractJsonService;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

@Path("/dashboard")
public class DashboardService extends AbstractJsonService {

	private static Client client;
	private static EtmConfiguration etmConfiguration;
	
	public static void initialize(Client client, EtmConfiguration etmConfiguration) {
		DashboardService.client = client;
		DashboardService.etmConfiguration = etmConfiguration;
	}
	
	
	@POST
	@Path("/chart")
	@Produces(MediaType.APPLICATION_JSON)
	public String getChartData(String json) {
		Map<String, Object> valueMap = toMap(json);
		String type = getString("type", valueMap);
		if ("line".equals(type)) {
			return getDataForLineChart(valueMap);
		}
		return null;
	}


	private String getDataForLineChart(Map<String, Object> valueMap) {
		Map<String, Object> indexValues = getObject("index", valueMap);
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(getString("name", indexValues))
			.setQuery(getEtmPrincipalFilterQuery())
			.setSize(0)
			.setFetchSource(false)
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		List<String> types = getArray("types", indexValues);
		if (types != null && types.size() > 0) {
			searchRequestBuilder.setTypes(types.toArray(new String[types.size()]));
		}
		
		
		Map<String, Object> xAxisValues = getObject("x_axis", valueMap);
		Map<String, Object> yAxisValues = getObject("y_axis", valueMap);
		
		Map<String, Object> xAxisAggregationValues = getObject("agg", xAxisValues);
		Map<String, Object> yAxisAggregationValues = getObject("agg", yAxisValues);

		AggregationBuilder yAxisAggregationBuilder = createAggregations(yAxisAggregationValues, false);
		
		AggregationBuilder xAxisAggregationBuilder = createAggregations(xAxisAggregationValues, true, yAxisAggregationBuilder);
		searchRequestBuilder.addAggregation(xAxisAggregationBuilder);
		
		SearchResponse searchResponse = searchRequestBuilder.get();
		
		
		InternalAggregation aggregation = searchResponse.getAggregations().get(getString("name", xAxisAggregationValues));
		try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            aggregation.toXContent(builder, ToXContent.EMPTY_PARAMS);
            builder.endObject();
            return builder.string();
        } catch (IOException e) {
        	throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }		
	}

	private AggregationBuilder createAggregations(Map<String, Object> aggregationValues, boolean addSubAggregations, AggregationBuilder appendingSubAggregation) {
		String type = getString("type", aggregationValues);
		AggregationBuilder aggregationBuilder = null;
		if ("date-histogram".equals(type)) {
			aggregationBuilder =  AggregationBuilders.dateHistogram(getString("name",aggregationValues))
				.timeZone(DateTimeZone.forID(getEtmPrincipal().getTimeZone().getID()))
				.field(getString("field", aggregationValues))
				.dateHistogramInterval(getDateHistogramInterval(getString("interval", aggregationValues)));
		} else if ("terms".equals(type)) {
			aggregationBuilder = AggregationBuilders.terms(getString("name",aggregationValues))
				.field(getString("field", aggregationValues))
				.size(getInteger("size", aggregationValues, 10));
		} else if ("stats".equals(type)) {
			aggregationBuilder = AggregationBuilders.stats(getString("name",aggregationValues))
				.field(getString("field", aggregationValues));
// Matrix aggregations			
		} else if ("avg".equals(type)) {
			aggregationBuilder = AggregationBuilders.avg(getString("name",aggregationValues))
				.field(getString("field", aggregationValues));
		} else if ("min".equals(type)) {
			aggregationBuilder = AggregationBuilders.min(getString("name",aggregationValues))
					.field(getString("field", aggregationValues));
		} else if ("max".equals(type)) {
			aggregationBuilder = AggregationBuilders.max(getString("name",aggregationValues))
					.field(getString("field", aggregationValues));
//		} else if ("percentiles".equals(type)) {
//			aggregationBuilder = AggregationBuilders.percentiles(getString("name",aggregationValues))
//					.field(getString("field", aggregationValues));			
		}
		if (aggregationBuilder != null) {
			if (addSubAggregations) {
				List<Map<String, Object>> subAggregations = getArray("aggs", aggregationValues);
				if (subAggregations != null) {
					for (Map<String, Object> subAggregation : subAggregations) {
						AggregationBuilder subAggregationBuilder = createAggregations(subAggregation, addSubAggregations, appendingSubAggregation);
						if (subAggregationBuilder != null) {
							aggregationBuilder.subAggregation(subAggregationBuilder);
						} 
					}
				} else if (appendingSubAggregation != null) {
					aggregationBuilder.subAggregation(appendingSubAggregation);
				}
			} else if (appendingSubAggregation != null) {
				aggregationBuilder.subAggregation(appendingSubAggregation);
			}
		}
		return aggregationBuilder;		
	}
	
	private AggregationBuilder createAggregations(Map<String, Object> aggregationValues, boolean addSubAggregations) {
		return createAggregations(aggregationValues, addSubAggregations, null);
	}


	private DateHistogramInterval getDateHistogramInterval(String interval) {
		if ("day".equals(interval)) {
			return DateHistogramInterval.DAY;
		} else if ("hour".equals(interval)) {
			return DateHistogramInterval.HOUR;
		} else if ("minute".equals(interval)) {
			return DateHistogramInterval.MINUTE;
		} else if ("month".equals(interval)) {
			return DateHistogramInterval.MONTH;
		} else if ("quarter".equals(interval)) {
			return DateHistogramInterval.QUARTER;
		} else if ("second".equals(interval)) {
			return DateHistogramInterval.SECOND;
		} else if ("week".equals(interval)) {
			return DateHistogramInterval.WEEK;
		} else if ("year".equals(interval)) {
			return DateHistogramInterval.YEAR;
		}
		return null;
	}
}
