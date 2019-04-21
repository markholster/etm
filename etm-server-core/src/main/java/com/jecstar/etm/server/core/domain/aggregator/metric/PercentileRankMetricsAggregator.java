package com.jecstar.etm.server.core.domain.aggregator.metric;

import com.jecstar.etm.server.core.converter.JsonField;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.PercentileRanksAggregationBuilder;

public class PercentileRankMetricsAggregator extends FieldBasedMetricsAggregator {

    public static final String TYPE = "percentile_rank";
    private static final String RANK = "rank";

    @JsonField(RANK)
    private Double rank;

    public PercentileRankMetricsAggregator() {
        super();
        setMetricsAggregatorType(TYPE);
    }

    public Double getRank() {
        return this.rank;
    }

    public PercentileRankMetricsAggregator setRank(Double rank) {
        this.rank = rank;
        return this;
    }

    @Override
    public PercentileRankMetricsAggregator clone() {
        PercentileRankMetricsAggregator clone = new PercentileRankMetricsAggregator();
        clone.rank = this.rank;
        super.clone(clone);
        return clone;
    }

    @Override
    public PercentileRanksAggregationBuilder toAggregationBuilder() {
        return AggregationBuilders.percentileRanks(getId(), new double[]{getRank()}).setMetaData(getMetadata()).field(getField());
    }
}
