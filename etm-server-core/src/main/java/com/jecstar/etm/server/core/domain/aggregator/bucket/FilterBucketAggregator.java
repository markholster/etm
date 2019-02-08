package com.jecstar.etm.server.core.domain.aggregator.bucket;

import com.jecstar.etm.server.core.converter.JsonField;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;

public class FilterBucketAggregator extends BucketAggregator {

    public static final String TYPE = "filter";

    private static final String VALUE = "value";

    @JsonField(VALUE)
    private String value;

    public FilterBucketAggregator() {
        super();
        setBucketAggregatorType(TYPE);
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public FilterBucketAggregator clone() {
        FilterBucketAggregator clone = new FilterBucketAggregator();
        clone.value = this.value;
        super.clone(clone);
        return clone;
    }

    @Override
    protected FilterAggregationBuilder createAggregationBuilder() {
        return AggregationBuilders.filter(getId(), QueryBuilders.termQuery(getFieldOrKeyword(), getValue()))
                .setMetaData(getMetadata());
    }

}
