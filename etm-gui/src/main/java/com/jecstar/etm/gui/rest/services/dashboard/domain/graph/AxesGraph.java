package com.jecstar.etm.gui.rest.services.dashboard.domain.graph;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.XAxisConverter;
import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.YAxisConverter;
import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.custom.EnumConverter;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.BucketAggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.DateHistogramBucketAggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.SignificantTermBucketAggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.TermBucketAggregator;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;

import java.util.stream.Collectors;

/**
 * Superclass for all <code>Graph</code> instances that use axes for their visual representation.
 */
public abstract class AxesGraph<T extends AxesGraph> extends Graph<T> {

    public enum Orientation {
        HORIZONTAL, VERTICAL;

        public static Orientation safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return Orientation.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public static final String X_AXIS = "x_axis";
    public static final String Y_AXIS = "y_axis";
    public static final String ORIENTATION = "orientation";
    public static final String SHOW_LEGEND = "show_legend";

    @JsonField(value = X_AXIS, converterClass = XAxisConverter.class)
    private XAxis xAxis;
    @JsonField(value = Y_AXIS, converterClass = YAxisConverter.class)
    private YAxis yAxis;
    @JsonField(value = ORIENTATION, converterClass = EnumConverter.class)
    private Orientation orientation;
    @JsonField(SHOW_LEGEND)
    private boolean showLegend;

    public XAxis getXAxis() {
        return this.xAxis;
    }

    public YAxis getYAxis() {
        return this.yAxis;
    }

    public Orientation getOrientation() {
        return this.orientation;
    }

    public boolean isShowLegend() {
        return this.showLegend;
    }

    @SuppressWarnings("unchecked")
    public T setXAxis(XAxis xAxis) {
        this.xAxis = xAxis;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setYAxis(YAxis yAxis) {
        this.yAxis = yAxis;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setOrientation(Orientation orientation) {
        this.orientation = orientation;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setShowLegend(boolean showLegend) {
        this.showLegend = showLegend;
        return (T) this;
    }

    @Override
    public void addAggregators(SearchRequestBuilder searchRequestBuilder) {
        BucketAggregator bucketAggregator = getXAxis().getBucketAggregator().clone();
        bucketAggregator.addAggregators(getYAxis().getAggregators().stream().map(Aggregator::clone).collect(Collectors.toList()));
        searchRequestBuilder.addAggregation(bucketAggregator.toAggregationBuilder());
    }

    @Override
    public void appendHighchartsConfig(JsonBuilder builder) {
        builder.startObject("legend").field("enabled", isShowLegend()).endObject();
        if (DateHistogramBucketAggregator.TYPE.equals(getXAxis().getBucketAggregator().getBucketAggregatorType())) {
            builder.startObject("xAxis").field("type", "datetime").endObject();
        } else if (TermBucketAggregator.TYPE.equals(getXAxis().getBucketAggregator().getBucketAggregatorType()) ||
                SignificantTermBucketAggregator.TYPE.equals(getXAxis().getBucketAggregator().getBucketAggregatorType())) {
            builder.startObject("xAxis").field("type", "category").endObject();
        }
        builder.startObject("yAxis").startObject("title").field("text", getYAxis().getTitle()).endObject().endObject();
    }

    @Override
    public String getValueFormat() {
        return getYAxis().getFormat();
    }

    @Override
    public void mergeFromColumn(T graph) {
        this.orientation = graph.getOrientation();
        this.showLegend = graph.isShowLegend();
    }

    @Override
    public void prepareForSearch(final DataRepository dataRepository, final SearchRequestBuilder searchRequestBuilder) {
        getXAxis().getBucketAggregator().prepareForSearch(dataRepository, searchRequestBuilder);
        getYAxis().getAggregators().stream().filter(p -> p instanceof BucketAggregator).forEach(c -> ((BucketAggregator) c).prepareForSearch(dataRepository, searchRequestBuilder));
    }
}

