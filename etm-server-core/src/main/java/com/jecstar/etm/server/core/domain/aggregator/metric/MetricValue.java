package com.jecstar.etm.server.core.domain.aggregator.metric;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.metrics.InternalNumericMetricsAggregation;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentileRanks;
import org.elasticsearch.search.aggregations.metrics.percentiles.Percentiles;

public class MetricValue {

    private final String jsonValue;
    private final String name;
    private final double value;

    public MetricValue(Aggregation aggregation) {
        if (aggregation instanceof Percentiles) {
            Percentiles percentiles = (Percentiles) aggregation;
            this.value = percentiles.iterator().next().getValue();
        } else if (aggregation instanceof PercentileRanks) {
            PercentileRanks percentileRanks = (PercentileRanks) aggregation;
            this.value = percentileRanks.iterator().next().getPercent();
        } else if (aggregation instanceof InternalNumericMetricsAggregation.SingleValue) {
            NumericMetricsAggregation.SingleValue singleValue = (InternalNumericMetricsAggregation.SingleValue) aggregation;
            this.value = singleValue.value();
        } else {
            throw new IllegalArgumentException("'" + aggregation.getClass().getName() + "' is an invalid metric aggregator.");
        }
        this.name = (String) aggregation.getMetaData().get("name");
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
