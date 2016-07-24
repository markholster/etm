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
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
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
		String type = getString("type", valueMap);
		if ("line".equals(type)) {
			return getDataForLineChart(valueMap);
		}
		return null;
	}


	private String getDataForLineChart(Map<String, Object> valueMap) {
		StringBuilder result = new StringBuilder();
		result.append("[");
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(getString("index", valueMap))
			.setSize(0)
			.setFetchSource(false)
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
		Map<String, Object> aggregationValues = getObject("agg", valueMap);
		AggregationBuilder aggregationBuilder = createAggregations(aggregationValues);
		if (aggregationBuilder != null) {
			searchRequestBuilder.addAggregation(aggregationBuilder);
		}
		SearchResponse searchResponse = searchRequestBuilder.get();
		Map<String, Object> xAxisValues = getObject("x_axis", aggregationValues);
		Map<String, Object> yAxisValues = getObject("y_axis", aggregationValues);
		String[] xSelectors = getString("selector", xAxisValues).split("->");
		String[] ySelectors = getString("selector", xAxisValues).split("->");
		
		Aggregations aggregations = searchResponse.getAggregations();
		Aggregation aggregation = aggregations.get("Events over time");
		
		
		result.append("]");
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
