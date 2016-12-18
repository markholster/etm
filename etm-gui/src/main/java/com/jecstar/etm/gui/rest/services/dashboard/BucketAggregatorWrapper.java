package com.jecstar.etm.gui.rest.services.dashboard;

import java.text.Format;
import java.util.Map;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

public class BucketAggregatorWrapper {
	
	private static final String DATE_FORMAT_ISO8601_WITHOUT_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	private final Map<String, Object> jsonData;
	private final JsonConverter jsonConverter = new JsonConverter();
	private final String aggregatorType;
	private final String field;
	private final String fieldType;
	private final Format fieldFormat;
	private final AggregationBuilder aggregationBuilder;

	public BucketAggregatorWrapper(EtmPrincipal etmPrincipal, Map<String, Object> jsonData) {
		this.jsonData = jsonData;
		this.aggregatorType = this.jsonConverter.getString("aggregator", jsonData);
		this.field = this.jsonConverter.getString("field", jsonData);
		this.fieldType = this.jsonConverter.getString("field_type", jsonData);
		if ("date_histogram".equals(this.aggregatorType)) {
			this.fieldFormat = Interval.safeValueOf(this.jsonConverter.getString("interval", jsonData)).getDateFormat(etmPrincipal.getLocale(), etmPrincipal.getTimeZone());
		} else {
			this.fieldFormat = "date".equals(this.fieldType) ? etmPrincipal.getDateFormat(DATE_FORMAT_ISO8601_WITHOUT_TIMEZONE) : etmPrincipal.getNumberFormat();
		}
		this.aggregationBuilder = createBucketAggregationBuilder();
	}
	
	
	private AggregationBuilder createBucketAggregationBuilder() {
		AggregationBuilder builder = null;
		if ("date_histogram".equals(this.aggregatorType)) {
			String internalLabel = "Date of " + this.field;
			Interval interval = Interval.safeValueOf(this.jsonConverter.getString("interval", this.jsonData));
			builder = AggregationBuilders.dateHistogram(internalLabel).field(this.field).dateHistogramInterval(interval.getDateHistogramInterval());		
		} else if ("term".equals(this.aggregatorType)) {
			String orderBy = this.jsonConverter.getString("order_by", this.jsonData, "term");
			String order = this.jsonConverter.getString("order", this.jsonData);
			int top = this.jsonConverter.getInteger("top", this.jsonData, 5);
			String internalLabel = "Top " + top + " of " + this.field;
			Terms.Order termsOrder = null;
			if ("term".equals(orderBy)) {
				termsOrder = Terms.Order.term("asc".equals(order));
			} else if (orderBy.startsWith("metric_")) {
				termsOrder = Terms.Order.aggregation(orderBy, "asc".equals(order));
			} else {
				throw new IllegalArgumentException("'" + orderBy + "' is an invalid term order.");
			}
			builder = AggregationBuilders.terms(internalLabel).field(this.field).order(termsOrder).size(top);
		}
		if (builder == null)  {
			throw new IllegalArgumentException("'" + this.aggregatorType + "' is an invalid bucket aggregator.");
		}
		return builder;
	}
	
	public AggregationBuilder getAggregationBuilder() {
		return this.aggregationBuilder;
	}
	
	public Format getBucketFormat() {
		return this.fieldFormat;
	}

}
