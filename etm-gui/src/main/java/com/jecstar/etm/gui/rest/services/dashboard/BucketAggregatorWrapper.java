package com.jecstar.etm.gui.rest.services.dashboard;

import com.jecstar.etm.gui.rest.AbstractJsonService;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.aggregations.metrics.min.Min;

import java.text.Format;
import java.util.HashMap;
import java.util.Map;

class BucketAggregatorWrapper {
	
	private static final String AGGREGATOR_BASE = "bucket";
	
	private static final String DATE_FORMAT_ISO8601_WITHOUT_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	private final Map<String, Object> jsonData;
	private final JsonConverter jsonConverter = new JsonConverter();
	private final String aggregatorType;
	private final String field;
    private final Format fieldFormat;
	private final AggregationBuilder aggregationBuilder;
	private DateInterval requiredDateInterval;

	public BucketAggregatorWrapper(EtmPrincipal etmPrincipal, Map<String, Object> jsonData) {
		this(etmPrincipal, jsonData, null);
	}
	
	public BucketAggregatorWrapper(EtmPrincipal etmPrincipal, Map<String, Object> jsonData, SearchRequestBuilder searchRequest) {
		this.jsonData = jsonData;
		this.aggregatorType = this.jsonConverter.getString("aggregator", jsonData);
        String fieldType = this.jsonConverter.getString("field_type", jsonData);
		this.field = "text".equals(fieldType) ? this.jsonConverter.getString("field", jsonData) + AbstractJsonService.KEYWORD_SUFFIX : this.jsonConverter.getString("field", jsonData);
        if ("date_histogram".equals(this.aggregatorType)) {
			if ("auto".equals(this.jsonConverter.getString("interval", jsonData))) {
				this.requiredDateInterval = calculateInterval(searchRequest, this.field);
			} else {
				this.requiredDateInterval = DateInterval.safeValueOf(this.jsonConverter.getString("interval", jsonData));
			}
			this.fieldFormat = this.requiredDateInterval.getDateFormat(etmPrincipal.getLocale(), etmPrincipal.getTimeZone());
		} else {
			this.fieldFormat = "date".equals(fieldType) ? etmPrincipal.getDateFormat(DATE_FORMAT_ISO8601_WITHOUT_TIMEZONE) : etmPrincipal.getNumberFormat();
		}
		this.aggregationBuilder = createBucketAggregationBuilder();
	}
	


	private DateInterval calculateInterval(SearchRequestBuilder searchRequest, String field) {
		SearchResponse searchResponse = searchRequest
			.addAggregation(AggregationBuilders.min("min").field(field))
			.addAggregation(AggregationBuilders.max("max").field(field))
			.get();
		long min = new Double(((Min)searchResponse.getAggregations().get("min")).getValue()).longValue();
		long max = new Double(((Max)searchResponse.getAggregations().get("max")).getValue()).longValue();
		return DateInterval.ofRange(max-min);
	}

	private AggregationBuilder createBucketAggregationBuilder() {
		Map<String, Object> metadata = new HashMap<>();
		AggregationBuilder builder = null;
		if ("date_histogram".equals(this.aggregatorType)) {
			String internalLabel = "Date of " + this.field;
			builder = AggregationBuilders.dateHistogram(internalLabel).field(this.field).dateHistogramInterval(this.requiredDateInterval.getDateHistogramInterval());		
		} else if ("histogram".equals(this.aggregatorType)) {
			Double interval = this.jsonConverter.getDouble("interval", this.jsonData, 1D);
			builder = AggregationBuilders.histogram(this.field).field(this.field).interval(interval);
		} else if ("significant_term".equals(this.aggregatorType)) {
			String internalLabel = "Unusual terms of " + this.field;
			int top = this.jsonConverter.getInteger("top", this.jsonData, 5);
			builder = AggregationBuilders.significantTerms(internalLabel).field(this.field).size(top);
		} else if ("term".equals(this.aggregatorType)) {
			String orderBy = this.jsonConverter.getString("order_by", this.jsonData, "term");
			String order = this.jsonConverter.getString("order", this.jsonData);
			int top = this.jsonConverter.getInteger("top", this.jsonData, 5);
			String internalLabel = "Top " + top + " of " + this.field;
			BucketOrder termsOrder;
			if ("term".equals(orderBy)) {
                termsOrder = BucketOrder.key("asc".equals(order));
			} else if (orderBy.startsWith("metric_")) {
				termsOrder = BucketOrder.aggregation(orderBy, "asc".equals(order));
			} else {
				throw new IllegalArgumentException("'" + orderBy + "' is an invalid term order.");
			}
			builder = AggregationBuilders.terms(internalLabel).field(this.field).order(termsOrder).size(top);
		}
		if (builder == null)  {
			throw new IllegalArgumentException("'" + this.aggregatorType + "' is an invalid bucket aggregator.");
		}
		metadata.put("aggregator_base", AGGREGATOR_BASE);
		builder.setMetaData(metadata);
		return builder;
	}
	
	public AggregationBuilder getAggregationBuilder() {
		return this.aggregationBuilder;
	}
	
	public Format getBucketFormat() {
		return this.fieldFormat;
	}

}
