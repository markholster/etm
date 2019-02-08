package com.jecstar.etm.gui.rest.services.dashboard.domain.graph;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.aggregator.bucket.BucketAggregator;
import com.jecstar.etm.server.core.domain.aggregator.converter.AggregatorConverter;

public class XAxis {

    private static final String BUCKET_AGGREGATOR = "aggregator";

    @JsonField(value = BUCKET_AGGREGATOR, converterClass = AggregatorConverter.class)
    private BucketAggregator bucketAggregator;

    public BucketAggregator getBucketAggregator() {
        return this.bucketAggregator;
    }

    public XAxis setBucketAggregator(BucketAggregator bucketAggregator) {
        this.bucketAggregator = bucketAggregator;
        return this;
    }
}
