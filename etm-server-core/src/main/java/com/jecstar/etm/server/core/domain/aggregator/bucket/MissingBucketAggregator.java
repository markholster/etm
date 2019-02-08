package com.jecstar.etm.server.core.domain.aggregator.bucket;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.missing.MissingAggregationBuilder;

public class MissingBucketAggregator extends BucketAggregator {

    public static final String TYPE = "missing";

    public MissingBucketAggregator() {
        super();
        setBucketAggregatorType(TYPE);
    }

    @Override
    public MissingBucketAggregator clone() {
        MissingBucketAggregator clone = new MissingBucketAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    protected MissingAggregationBuilder createAggregationBuilder() {
        return AggregationBuilders.missing(getId()).field(getField()).setMetaData(getMetadata());
    }

}
