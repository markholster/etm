package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.BucketAggregatorConverter;
import com.jecstar.etm.server.core.converter.JsonField;

public class XAxis {

    private static final String BUCKET_AGGREGATOR = "aggregator";
    private static final String BUCKET_SUBAGGREGATOR = "sub_aggregator";

    @JsonField(value = BUCKET_AGGREGATOR, converterClass = BucketAggregatorConverter.class)
    private BucketAggregator bucketAggregator;

    @JsonField(value = BUCKET_SUBAGGREGATOR, converterClass = BucketAggregatorConverter.class)
    private BucketAggregator bucketSubAggregator;

    public BucketAggregator getBucketAggregator() {
        return this.bucketAggregator;
    }

    public BucketAggregator getBucketSubAggregator() {
        return this.bucketSubAggregator;
    }
}
