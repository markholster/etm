package com.jecstar.etm.server.core.domain.aggregator.bucket;

import com.jecstar.etm.server.core.converter.JsonField;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.significant.SignificantTermsAggregationBuilder;

public class SignificantTermBucketAggregator extends BucketAggregator {

    public static final String TYPE = "significant_term";

    private static final String TOP = "top";
    private static final String MIN_DOC_COUNT = "min_doc_count";


    @JsonField(TOP)
    private Integer top;
    @JsonField(MIN_DOC_COUNT)
    private Long minDocCount;

    public SignificantTermBucketAggregator setTop(Integer top) {
        this.top = top;
        return this;
    }

    public SignificantTermBucketAggregator setMinDocCount(Long minDocCount) {
        this.minDocCount = minDocCount;
        return this;
    }

    public SignificantTermBucketAggregator() {
        super();
        setBucketAggregatorType(TYPE);
    }

    public Integer getTop() {
        return this.top != null ? this.top : 5;
    }

    public Long getMinDocCount() {
        return this.minDocCount;
    }

    @Override
    public SignificantTermBucketAggregator clone() {
        SignificantTermBucketAggregator clone = new SignificantTermBucketAggregator();
        clone.top = this.top;
        clone.minDocCount = this.minDocCount;
        super.clone(clone);
        return clone;
    }

    @Override
    protected SignificantTermsAggregationBuilder createAggregationBuilder() {
        SignificantTermsAggregationBuilder builder = AggregationBuilders.significantTerms(getId())
                .setMetaData(getMetadata())
                .field(getFieldOrKeyword()).size(getTop());
        if (getMinDocCount() != null) {
            builder.minDocCount(getMinDocCount());
        }
        return builder;
    }

    @Override
    protected boolean isBucketKeyNumeric() {
        return false;
    }
}
