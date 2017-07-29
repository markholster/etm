package com.jecstar.etm.gui.rest.services.dashboard;

import java.text.Format;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

class MetricAggregatorWrapper {

	private static final String AGGREGATOR_BASE = "metric";
	
	private static final String DATE_FORMAT_ISO8601_WITHOUT_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	private final Map<String, Object> jsonData;
	private final JsonConverter jsonConverter = new JsonConverter();
	private final String aggregatorType;
	private final String id;
	private final String field;
    private final String label;
	private final Format fieldFormat;
	private final AggregationBuilder aggregationBuilder;
	
	public MetricAggregatorWrapper(EtmPrincipal etmPrincipal, Map<String, Object> jsonData) {
		this.jsonData = jsonData;
		this.aggregatorType = this.jsonConverter.getString("aggregator", jsonData);
		this.id = this.jsonConverter.getString("id", jsonData);
		this.field = this.jsonConverter.getString("field", jsonData);
        String fieldType = this.jsonConverter.getString("field_type", jsonData);
		this.label = this.jsonConverter.getString("label", jsonData);
		this.fieldFormat = "date".equals(fieldType) ? etmPrincipal.getDateFormat(DATE_FORMAT_ISO8601_WITHOUT_TIMEZONE) : etmPrincipal.getNumberFormat();
		this.aggregationBuilder = createMetricAggregationBuilder(etmPrincipal);
	}
	
	private AggregationBuilder createMetricAggregationBuilder(EtmPrincipal etmPrincipal) {
		Map<String, Object> metadata = new HashMap<>();
		AggregationBuilder builder = null;
		if ("average".equals(this.aggregatorType)) {
			metadata.put("label", this.label != null ? this.label : "Average of " + this.field);
			builder = AggregationBuilders.avg(this.id).field(this.field);
		} else if ("cardinality".equals(this.aggregatorType)) {
			// TODO beschrijven dat dit niet een precieze waarde is: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-metrics-cardinality-aggregation.html
			metadata.put("label", this.label != null ? this.label : "Unique count of " + this.field);
			builder = AggregationBuilders.cardinality(this.id).field(this.field);
		} else if ("count".equals(this.aggregatorType)) {
			metadata.put("label", this.label != null ? this.label : "Count");
			builder = AggregationBuilders.count(this.id).field("_type");
		} else if ("max".equals(this.aggregatorType)) {
			metadata.put("label", this.label != null ? this.label : "Max of " + this.field);
			builder = AggregationBuilders.max(this.id).field(this.field);			
		} else if ("median".equals(this.aggregatorType)) {
			metadata.put("label", this.label != null ? this.label : "Median of " + this.field);
			builder = AggregationBuilders.percentiles(this.id).field(this.field).percentiles(50);			
		} else if ("min".equals(this.aggregatorType)) {
			metadata.put("label", this.label != null ? this.label : "Min of " + this.field);
			builder = AggregationBuilders.min(this.id).field(this.field);			
		} else if ("percentile".equals(this.aggregatorType)) {
			String internalLabel = this.label;
			Double percentileData = this.jsonConverter.getDouble("percentile_data", this.jsonData);
			if (internalLabel == null) {
				if (percentileData == 1) {
					internalLabel = "1st percentile of " + this.field;
				} else if (percentileData == 2) {
					internalLabel = "2nd percentile of " + this.field;
				} else if (percentileData == 3) {
					internalLabel = "3rd percentile of " + this.field;
				} else {
					internalLabel = etmPrincipal.getNumberFormat().format(percentileData) + "th percentile of " + this.field;
				}
			}
			metadata.put("label", internalLabel);
			builder = AggregationBuilders.percentiles(this.id).field(this.field).percentiles(percentileData);			
		} else if ("percentile_rank".equals(this.aggregatorType)) {
			Double percentileData = this.jsonConverter.getDouble("percentile_data", this.jsonData);
			metadata.put("label", this.label != null ? this.label : "Percentile rank " + etmPrincipal.getNumberFormat().format(percentileData) + " of " + this.field);
			builder = AggregationBuilders.percentileRanks(this.id).field(this.field).values(percentileData);
		} else if ("sum".equals(this.aggregatorType)) {
			metadata.put("label", this.label != null ? this.label : "Sum of " + this.field);
			builder = AggregationBuilders.sum(this.id).field(this.field);
		}
		if (builder == null) {
			throw new IllegalArgumentException("'" + this.aggregatorType + "' is an invalid metric aggregator.");
		}
		metadata.put("aggregator_base", AGGREGATOR_BASE);
		builder.setMetaData(metadata);
		return builder;
	}

	public AggregationBuilder getAggregationBuilder() {
		return this.aggregationBuilder;
	}
	
	public String getAggregatorType() {
		return this.aggregatorType;
	}
	
	public Format getFieldFormat() {
		return this.fieldFormat;
	}
}
