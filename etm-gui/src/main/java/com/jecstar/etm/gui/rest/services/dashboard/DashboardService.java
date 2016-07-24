package com.jecstar.etm.gui.rest.services.dashboard;

import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation.Bucket;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.jecstar.etm.gui.rest.AbstractJsonService;
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
		Map<String, Object> dataValues = getObject("data", valueMap);
		String type = getString("type", dataValues);
		if ("line".equals(type)) {
			return getDataForLineChart(valueMap);
		}
		return null;
	}


	private String getDataForLineChart(Map<String, Object> valueMap) {
		Map<String, Object> dataValues = getObject("data", valueMap);
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(getString("index", dataValues))
			.setQuery(getEtmPrincipalFilterQuery())
			.setSize(0)
			.setFetchSource(false)
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		Map<String, Object> aggregationValues = getObject("agg", dataValues);
		AggregationBuilder aggregationBuilder = createAggregations(aggregationValues);
		if (aggregationBuilder != null) {
			searchRequestBuilder.addAggregation(aggregationBuilder);
		}
		SearchResponse searchResponse = searchRequestBuilder.get();
		Map<String, Object> xAxisValues = getObject("x_axis", valueMap);
		Map<String, Object> yAxisValues = getObject("y_axis", valueMap);
		String[] xSelectors = getString("selector", xAxisValues).split("->");
		String[] ySelectors = getString("selector", yAxisValues).split("->");
		MultiBucketsAggregation aggregation = searchResponse.getAggregations().get(getString("name",aggregationValues));
		StringBuilder result = new StringBuilder();
		result.append("[{");
		addStringElementToJsonBuffer("key", getString("name",aggregationValues), result, true);
		result.append(", \"values\": [");
		boolean first = true;
		for (Bucket bucket : aggregation.getBuckets()) {
			if (!first) {
				result.append(",");
			}
			result.append("[");
			Object key = bucket.getKey();
			if (key instanceof DateTime) {
				result.append(((DateTime)key).getMillis());
			} else if (key instanceof Number) {
				result.append(((Number)key).longValue());
			} else {
				result.append(escapeToJson(bucket.getKeyAsString(), true));
			}
			result.append("," + bucket.getDocCount());
			result.append("]");
			first = false;
		}
		result.append("]}]");
		return result.toString();
	}


	private AggregationBuilder createAggregations(Map<String, Object> aggregationValues) {
		String type = getString("type", aggregationValues);
		if ("date-histogram".equals(type)) {
			return AggregationBuilders.dateHistogram(getString("name",aggregationValues))
				.timeZone(DateTimeZone.forID(getEtmPrincipal().getTimeZone().getID()))
				.field(getString("field", aggregationValues))
				.dateHistogramInterval(getDateHistogramInterval(getString("interval", aggregationValues)));
		}
		return null;
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
