package com.jecstar.etm.server.core.domain.aggregator.bucket;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import com.jecstar.etm.server.core.domain.aggregator.converter.AggregatorListConverter;
import com.jecstar.etm.server.core.domain.aggregator.metric.MetricsAggregator;
import com.jecstar.etm.server.core.domain.aggregator.pipeline.PipelineAggregator;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BucketAggregator extends Aggregator {

    public static final String TYPE = "bucket";
    public static final String BUCKET_AGGREGATOR_TYPE = "bucket_type";
    private static final String FIELD = "field";
    private static final String FIELD_TYPE = "field_type";
    private static final String AGGREGATORS = "aggregators";

    @JsonField(BUCKET_AGGREGATOR_TYPE)
    private String bucketAggregatorType;
    @JsonField(FIELD)
    private String field;
    @JsonField(FIELD_TYPE)
    private String fieldType;
    @JsonField(value = AGGREGATORS, converterClass = AggregatorListConverter.class)
    private List<Aggregator> aggregators;

    public BucketAggregator() {
        super();
        setAggregatorType(TYPE);
    }

    public String getBucketAggregatorType() {
        return this.bucketAggregatorType;
    }

    public String getField() {
        return this.field;
    }

    public String getFieldType() {
        return this.fieldType;
    }

    public List<Aggregator> getAggregators() {
        return this.aggregators;
    }

    protected BucketAggregator setBucketAggregatorType(String bucketAggregatorType) {
        this.bucketAggregatorType = bucketAggregatorType;
        return this;
    }

    public BucketAggregator setField(String field) {
        this.field = field;
        return this;
    }

    public BucketAggregator setFieldType(String fieldType) {
        this.fieldType = fieldType;
        return this;
    }

    public BucketAggregator setAggregators(List<Aggregator> aggregators) {
        this.aggregators = aggregators;
        return this;
    }

    public String getFieldOrKeyword() {
        return "text".equals(getFieldType()) ? getField() + Aggregator.KEYWORD_SUFFIX : getField();
    }

    @Override
    public BucketAggregator clone() {
        throw new UnsupportedOperationException("Clone method must be implemented in '" + getClass().getName() + ".");
    }

    protected void clone(BucketAggregator target) {
        super.clone(target);
        target.bucketAggregatorType = this.bucketAggregatorType;
        target.field = this.field;
        target.fieldType = this.fieldType;
        if (this.aggregators != null) {
            target.aggregators = this.aggregators.stream().map(d -> d.clone()).collect(Collectors.toList());
        }
    }

    public void addAggregators(List<Aggregator> aggregators) {
        if (aggregators == null || aggregators.isEmpty()) {
            return;
        }
        if (this.aggregators == null) {
            this.aggregators = new ArrayList<>();
        }
        this.aggregators.addAll(aggregators);
    }

    @Override
    public final AggregationBuilder toAggregationBuilder() {
        AggregationBuilder aggregationBuilder = createAggregationBuilder();
        for (Aggregator aggregator : this.aggregators) {
            if (aggregator instanceof BucketAggregator) {
                aggregationBuilder.subAggregation(((BucketAggregator) aggregator).toAggregationBuilder());
            } else if (aggregator instanceof MetricsAggregator) {
                aggregationBuilder.subAggregation(((MetricsAggregator) aggregator).toAggregationBuilder());
            } else if (aggregator instanceof PipelineAggregator) {
                aggregationBuilder.subAggregation(((PipelineAggregator) aggregator).toAggregationBuilder());
            }
        }
        return aggregationBuilder;
    }

    protected abstract AggregationBuilder createAggregationBuilder();

    public void prepareForSearch(SearchRequestBuilder searchRequest) {
        getAggregators().stream().filter(p -> p instanceof BucketAggregator).forEach(c -> ((BucketAggregator) c).prepareForSearch(searchRequest));
    }

}
