package com.jecstar.etm.server.core.domain.aggregator.metric;

import com.jecstar.etm.server.core.converter.JsonField;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.PercentilesAggregationBuilder;

public class PercentileMetricsAggregator extends FieldBasedMetricsAggregator {

    public static final String TYPE = "percentile";
    private static final String PERCENTILE = "percentile";

    @JsonField(PERCENTILE)
    private Double percentile;

    public PercentileMetricsAggregator() {
        super();
        setMetricsAggregatorType(TYPE);
    }

    public Double getPercentile() {
        return this.percentile;
    }

    public PercentileMetricsAggregator setPercentile(Double percentile) {
        this.percentile = percentile;
        return this;
    }

    @Override
    public PercentileMetricsAggregator clone() {
        PercentileMetricsAggregator clone = new PercentileMetricsAggregator();
        clone.percentile = this.percentile;
        super.clone(clone);
        return clone;
    }

    @Override
    public PercentilesAggregationBuilder toAggregationBuilder() {
        return AggregationBuilders.percentiles(getId()).setMetaData(getMetadata()).field(getField()).percentiles(getPercentile());
    }
}
