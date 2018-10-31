package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import java.text.Format;
import java.util.HashMap;
import java.util.Map;

public class MetricAggregatorWrapper {

    private static final String AGGREGATOR_BASE = "metric";

    private static final String DATE_FORMAT_ISO8601_WITHOUT_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private final MetricAggregator metricAggregator;
    private final Format fieldFormat;
    private final AggregationBuilder aggregationBuilder;
    private final String id;

    public MetricAggregatorWrapper(EtmPrincipal etmPrincipal, MetricAggregator metricAggregator) {
        this(etmPrincipal, metricAggregator, metricAggregator.getId());
    }

    MetricAggregatorWrapper(EtmPrincipal etmPrincipal, MetricAggregator metricAggregator, String id) {
        this.id = id;
        this.metricAggregator = metricAggregator;
        this.fieldFormat = "date".equals(metricAggregator.getFieldType()) ? etmPrincipal.getDateFormat(DATE_FORMAT_ISO8601_WITHOUT_TIMEZONE) : etmPrincipal.getNumberFormat();
        this.aggregationBuilder = createMetricAggregationBuilder(etmPrincipal);
    }

    private AggregationBuilder createMetricAggregationBuilder(EtmPrincipal etmPrincipal) {
        Map<String, Object> metadata = new HashMap<>();
        AggregationBuilder builder = null;
        if ("average".equals(this.metricAggregator.getAggregatorType())) {
            metadata.put("label", this.metricAggregator.getLabel() != null ? this.metricAggregator.getLabel() : "Average of " + this.metricAggregator.getField());
            builder = AggregationBuilders.avg(this.id).field(this.metricAggregator.getField());
        } else if ("cardinality".equals(this.metricAggregator.getAggregatorType())) {
            // TODO beschrijven dat dit niet een precieze waarde is: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-metrics-cardinality-aggregation.html
            metadata.put("label", this.metricAggregator.getLabel() != null ? this.metricAggregator.getLabel() : "Unique count of " + this.metricAggregator.getField());
            builder = AggregationBuilders.cardinality(this.id).field(this.metricAggregator.getField());
        } else if ("count".equals(this.metricAggregator.getAggregatorType())) {
            metadata.put("label", this.metricAggregator.getLabel() != null ? this.metricAggregator.getLabel() : "Count");
            builder = AggregationBuilders.count(this.id).field("_type");
        } else if ("max".equals(this.metricAggregator.getAggregatorType())) {
            metadata.put("label", this.metricAggregator.getLabel() != null ? this.metricAggregator.getLabel() : "Max of " + this.metricAggregator.getField());
            builder = AggregationBuilders.max(this.id).field(this.metricAggregator.getField());
        } else if ("median".equals(this.metricAggregator.getAggregatorType())) {
            metadata.put("label", this.metricAggregator.getLabel() != null ? this.metricAggregator.getLabel() : "Median of " + this.metricAggregator.getField());
            builder = AggregationBuilders.percentiles(this.id).field(this.metricAggregator.getField()).percentiles(50);
        } else if ("min".equals(this.metricAggregator.getAggregatorType())) {
            metadata.put("label", this.metricAggregator.getLabel() != null ? this.metricAggregator.getLabel() : "Min of " + this.metricAggregator.getField());
            builder = AggregationBuilders.min(this.id).field(this.metricAggregator.getField());
        } else if ("percentile".equals(this.metricAggregator.getAggregatorType())) {
            String internalLabel = this.metricAggregator.getLabel();
            Double percentileData = this.metricAggregator.getPercentileData();
            if (internalLabel == null) {
                if (percentileData == 1) {
                    internalLabel = "1st percentile of " + this.metricAggregator.getField();
                } else if (percentileData == 2) {
                    internalLabel = "2nd percentile of " + this.metricAggregator.getField();
                } else if (percentileData == 3) {
                    internalLabel = "3rd percentile of " + this.metricAggregator.getField();
                } else {
                    internalLabel = etmPrincipal.getNumberFormat().format(percentileData) + "th percentile of " + this.metricAggregator.getField();
                }
            }
            metadata.put("label", internalLabel);
            builder = AggregationBuilders.percentiles(this.id).field(this.metricAggregator.getField()).percentiles(percentileData);
        } else if ("percentile_rank".equals(this.metricAggregator.getAggregatorType())) {
            Double percentileData = this.metricAggregator.getPercentileData();
            metadata.put("label", this.metricAggregator.getLabel() != null ? this.metricAggregator.getLabel() : "Percentile rank " + etmPrincipal.getNumberFormat().format(percentileData) + " of " + this.metricAggregator.getField());
            builder = AggregationBuilders.percentileRanks(this.id, new double[]{percentileData}).field(this.metricAggregator.getField());
        } else if ("sum".equals(this.metricAggregator.getAggregatorType())) {
            metadata.put("label", this.metricAggregator.getLabel() != null ? this.metricAggregator.getLabel() : "Sum of " + this.metricAggregator.getField());
            builder = AggregationBuilders.sum(this.id).field(this.metricAggregator.getField());
        }
        if (builder == null) {
            throw new IllegalArgumentException("'" + this.metricAggregator.getAggregatorType() + "' is an invalid metric aggregator.");
        }
        metadata.put("aggregator_base", AGGREGATOR_BASE);
        builder.setMetaData(metadata);
        return builder;
    }

    public AggregationBuilder getAggregationBuilder() {
        return this.aggregationBuilder;
    }

    public String getAggregatorType() {
        return this.metricAggregator.getAggregatorType();
    }

    public Format getFieldFormat() {
        return this.fieldFormat;
    }

    public String getId() {
        return this.id;
    }
}
