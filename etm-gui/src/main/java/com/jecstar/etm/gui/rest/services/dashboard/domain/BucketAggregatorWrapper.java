package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.rest.AbstractJsonService;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.aggregations.metrics.min.Min;

import java.text.Format;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BucketAggregatorWrapper {

    private static final String AGGREGATOR_BASE = "bucket";
    public static final String SORT_METRIC_ID = "sort_metric";

    private static final String DATE_FORMAT_ISO8601_WITHOUT_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private final EtmPrincipal etmPrincipal;
    private final BucketAggregator bucketAggregator;
    private final String field;
    private final Format fieldFormat;
    private final SearchRequestBuilder searchRequest;
    private AggregationBuilder aggregationBuilder;
    private MetricAggregatorWrapper sortOverrideAggregator;

    public BucketAggregatorWrapper(EtmPrincipal etmPrincipal, BucketAggregator bucketAggregator) {
        this(etmPrincipal, bucketAggregator, null);
    }

    public BucketAggregatorWrapper(EtmPrincipal etmPrincipal, BucketAggregator bucketAggregator, SearchRequestBuilder searchRequest) {
        this.etmPrincipal = etmPrincipal;
        this.bucketAggregator = bucketAggregator;
        this.searchRequest = searchRequest;
        this.field = "text".equals(this.bucketAggregator.getFieldType()) ? this.bucketAggregator.getField() + AbstractJsonService.KEYWORD_SUFFIX : this.bucketAggregator.getField();
        this.fieldFormat = "date".equals(this.bucketAggregator.getFieldType()) ? etmPrincipal.getDateFormat(DATE_FORMAT_ISO8601_WITHOUT_TIMEZONE) : etmPrincipal.getNumberFormat();
    }


    private DateInterval calculateInterval(SearchRequestBuilder searchRequest, String field) {
        SearchResponse searchResponse = searchRequest
                .addAggregation(AggregationBuilders.min("min").field(field))
                .addAggregation(AggregationBuilders.max("max").field(field))
                .get();
        long min = new Double(((Min) searchResponse.getAggregations().get("min")).getValue()).longValue();
        long max = new Double(((Max) searchResponse.getAggregations().get("max")).getValue()).longValue();
        return DateInterval.ofRange(max - min);
    }

    private AggregationBuilder createBucketAggregationBuilder() {
        Map<String, Object> metadata = new HashMap<>();
        AggregationBuilder builder = null;
        if (DateHistogramBucketAggregator.TYPE.equals(this.bucketAggregator.getAggregatorType())) {
            String internalLabel = "Date of " + this.field;
            builder = AggregationBuilders.dateHistogram(internalLabel)
                    .field(this.field)
                    .dateHistogramInterval(((DateHistogramBucketAggregator) this.bucketAggregator).getDateInterval(() -> calculateInterval(this.searchRequest, this.field)).getDateHistogramInterval());
        } else if (HistogramBucketAggregator.TYPE.equals(this.bucketAggregator.getAggregatorType())) {
            Double interval = ((HistogramBucketAggregator) this.bucketAggregator).getInterval();
            builder = AggregationBuilders.histogram(this.field).field(this.field).interval(interval);
        } else if (SignificantTermBucketAggregator.TYPE.equals(this.bucketAggregator.getAggregatorType())) {
            String internalLabel = "Unusual terms of " + this.field;
            int top = ((SignificantTermBucketAggregator) this.bucketAggregator).getTop();
            builder = AggregationBuilders.significantTerms(internalLabel).field(this.field).size(top);
        } else if (TermBucketAggregator.TYPE.equals(this.bucketAggregator.getAggregatorType())) {
            TermBucketAggregator termBucketAggregator = (TermBucketAggregator) this.bucketAggregator;
            int top = termBucketAggregator.getTop();
            String internalLabel = "Top " + top + " of " + this.field;
            BucketOrder termsOrder;
            if ("term".equals(termBucketAggregator.getOrderBy())) {
                termsOrder = BucketOrder.key("asc".equals(termBucketAggregator.getOrder()));
            } else if (termBucketAggregator.getOrderBy().startsWith("metric_") || SORT_METRIC_ID.equals(termBucketAggregator.getOrderBy())) {
                termsOrder = BucketOrder.aggregation(termBucketAggregator.getOrderBy(), "asc".equals(termBucketAggregator.getOrder()));
            } else {
                throw new IllegalArgumentException("'" + termBucketAggregator.getOrderBy() + "' is an invalid term order.");
            }
            builder = AggregationBuilders.terms(internalLabel).field(this.field).order(termsOrder).size(top);
            if (this.sortOverrideAggregator != null) {
                builder.subAggregation(this.sortOverrideAggregator.getAggregationBuilder());
            }
        }
        if (builder == null) {
            throw new IllegalArgumentException("'" + this.bucketAggregator.getAggregatorType() + "' is an invalid bucket aggregator.");
        }
        metadata.put("aggregator_base", AGGREGATOR_BASE);
        builder.setMetaData(metadata);
        return builder;
    }

    public AggregationBuilder getAggregationBuilder() {
        if (this.aggregationBuilder == null) {
            this.aggregationBuilder = createBucketAggregationBuilder();
        }
        return this.aggregationBuilder;
    }

    public Format getBucketFormat() {
        return this.fieldFormat;
    }


    public boolean needsMetricSubAggregatorForSorting() {
        if (TermBucketAggregator.TYPE.equals(this.bucketAggregator.getAggregatorType())) {
            TermBucketAggregator termBucketAggregator = (TermBucketAggregator) this.bucketAggregator;
            return termBucketAggregator.getOrderBy() != null && termBucketAggregator.getOrderBy().startsWith("metric_");
        }
        return false;
    }

    public void setSortOverrideAggregator(List<MetricAggregator> metricAggregators) {
        if (!needsMetricSubAggregatorForSorting()) {
            throw new IllegalStateException();
        }
        if (TermBucketAggregator.TYPE.equals(this.bucketAggregator.getAggregatorType())) {
            TermBucketAggregator termBucketAggregator = (TermBucketAggregator) this.bucketAggregator;
            for (MetricAggregator metricAggregator : metricAggregators) {
                if (termBucketAggregator.getOrderBy().equals(metricAggregator.getId())) {
                    termBucketAggregator.setOrderBy(SORT_METRIC_ID);
                    this.sortOverrideAggregator = new MetricAggregatorWrapper(this.etmPrincipal, metricAggregator, SORT_METRIC_ID);
                    break;
                }
            }
        }
    }
}
