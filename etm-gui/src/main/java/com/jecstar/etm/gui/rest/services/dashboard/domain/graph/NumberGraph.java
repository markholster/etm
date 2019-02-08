package com.jecstar.etm.gui.rest.services.dashboard.domain.graph;

import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.YAxisConverter;
import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.BucketAggregator;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.BaseAggregationBuilder;
import org.elasticsearch.search.aggregations.PipelineAggregationBuilder;

public class NumberGraph extends Graph<NumberGraph> {

    public static final String TYPE = "number";
    public static final String Y_AXIS = "y_axis";

    @JsonField(value = Y_AXIS, converterClass = YAxisConverter.class)
    private YAxis yAxis;

    public NumberGraph() {
        super();
        setType(TYPE);
    }

    public YAxis getYAxis() {
        return this.yAxis;
    }

    public NumberGraph setyAxis(YAxis yAxis) {
        this.yAxis = yAxis;
        return this;
    }

    @Override
    public void addAggregators(SearchRequestBuilder searchRequest) {
        for (Aggregator aggregator : getYAxis().getAggregators()) {
            BaseAggregationBuilder baseAggregationBuilder = aggregator.toAggregationBuilder();
            if (baseAggregationBuilder instanceof AggregationBuilder) {
                searchRequest.addAggregation((AggregationBuilder) baseAggregationBuilder);
            } else if (baseAggregationBuilder instanceof PipelineAggregationBuilder) {
                searchRequest.addAggregation((PipelineAggregationBuilder) baseAggregationBuilder);
            } else {
                throw new IllegalArgumentException("Unknown aggregation builder '" + baseAggregationBuilder.getClass().getName() + "'.");
            }
        }
    }

    @Override
    public void appendHighchartsConfig(StringBuilder config) {
    }

    @Override
    public String getValueFormat() {
        return getYAxis().getFormat();
    }

    @Override
    public void mergeFromColumn(NumberGraph graph) {
    }

    public void prepareForSearch(SearchRequestBuilder searchRequest) {
        getYAxis().getAggregators().stream().filter(p -> p instanceof BucketAggregator).forEach(c -> ((BucketAggregator) c).prepareForSearch(searchRequest));
    }
}
