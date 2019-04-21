package com.jecstar.etm.server.core.domain.aggregator.metric;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation;
import org.elasticsearch.search.aggregations.metrics.ParsedPercentiles;
import org.elasticsearch.search.aggregations.metrics.ParsedTDigestPercentileRanks;

public class MetricValue {

    private final String jsonValue;
    private final String name;
    private final double value;

    public MetricValue(Aggregation aggregation) {
        if (aggregation instanceof ParsedTDigestPercentileRanks) {
            ParsedTDigestPercentileRanks percentileRanks = (ParsedTDigestPercentileRanks) aggregation;
            this.value = percentileRanks.iterator().next().getPercent();
        } else if (aggregation instanceof ParsedPercentiles) {
            ParsedPercentiles percentiles = (ParsedPercentiles) aggregation;
            this.value = percentiles.iterator().next().getValue();
        } else if (aggregation instanceof NumericMetricsAggregation.SingleValue) {
            NumericMetricsAggregation.SingleValue singleValue = (NumericMetricsAggregation.SingleValue) aggregation;
            this.value = singleValue.value();
        } else {
            throw new IllegalArgumentException("'" + aggregation.getClass().getName() + "' is an invalid metric aggregator.");
        }
        this.name = (String) aggregation.getMetaData().get("name");
        this.jsonValue = formatToDoubleValue(this.value);
    }

    public MetricValue(String name, double value) {
        this.name = name;
        this.value = value;
        this.jsonValue = formatToDoubleValue(this.value);
    }

    private String formatToDoubleValue(double value) {
        if (!hasValidValue()) {
            return "null";
        }
        return Double.toString(value);
    }

    public String getJsonValue() {
        return this.jsonValue;
    }

    public String getName() {
        return this.name;
    }

    public Double getValue() {
        return this.value;
    }

    public boolean hasValidValue() {
        return !(Double.isNaN(this.value) || Double.isInfinite(this.value));
    }
}
