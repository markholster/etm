package com.jecstar.etm.gui.rest.services.dashboard.domain.graph;

import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.XAxisConverter;
import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.YAxisConverter;
import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.custom.EnumConverter;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.BucketAggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.DateHistogramBucketAggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.SignificantTermBucketAggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.TermBucketAggregator;
import org.elasticsearch.action.search.SearchRequestBuilder;

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
    public void addAggregators(SearchRequestBuilder searchRequest) {
        BucketAggregator bucketAggregator = getXAxis().getBucketAggregator().clone();
        bucketAggregator.addAggregators(getYAxis().getAggregators().stream().map(Aggregator::clone).collect(Collectors.toList()));
        searchRequest.addAggregation(bucketAggregator.toAggregationBuilder());
    }

    @Override
    public void appendHighchartsConfig(StringBuilder config) {
        config.append(", \"legend\": {\"enabled\": " + isShowLegend() + "}");
        if (DateHistogramBucketAggregator.TYPE.equals(getXAxis().getBucketAggregator().getBucketAggregatorType())) {
            config.append(", \"xAxis\": {\"type\": \"datetime\"}");
        } else if (TermBucketAggregator.TYPE.equals(getXAxis().getBucketAggregator().getBucketAggregatorType()) ||
                SignificantTermBucketAggregator.TYPE.equals(getXAxis().getBucketAggregator().getBucketAggregatorType())) {
            config.append(", \"xAxis\": {\"type\": \"category\"}");
        }
        config.append(", \"yAxis\": { \"title\": { \"text\": " + this.jsonWriter.escapeToJson(getYAxis().getTitle(), true) + "}}");
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
    public void prepareForSearch(SearchRequestBuilder searchRequest) {
        getXAxis().getBucketAggregator().prepareForSearch(searchRequest);
        getYAxis().getAggregators().stream().filter(p -> p instanceof BucketAggregator).forEach(c -> ((BucketAggregator) c).prepareForSearch(searchRequest));
    }
}

