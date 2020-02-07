package com.jecstar.etm.gui.rest.services.dashboard.domain.graph;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.XAxisConverter;
import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.YAxisConverter;
import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.BucketAggregator;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;

import java.util.stream.Collectors;

public class PieGraph extends Graph<PieGraph> {

    public static final String TYPE = "pie";
    public static final String SUB_TYPE = "sub_type";
    public static final String X_AXIS = "x_axis";
    public static final String Y_AXIS = "y_axis";
    public static final String SHOW_LEGEND = "show_legend";
    public static final String SHOW_DATA_LABELS = "show_data_labels";


    @JsonField(SUB_TYPE)
    private String subType;
    @JsonField(value = X_AXIS, converterClass = XAxisConverter.class)
    private XAxis xAxis;
    @JsonField(value = Y_AXIS, converterClass = YAxisConverter.class)
    private YAxis yAxis;
    @JsonField(SHOW_LEGEND)
    private boolean showLegend;
    @JsonField(SHOW_DATA_LABELS)
    private boolean showDataLabels;

    public PieGraph() {
        super();
        setType(TYPE);
    }

    public String getSubType() {
        return this.subType;
    }

    public XAxis getXAxis() {
        return this.xAxis;
    }

    public YAxis getYAxis() {
        return this.yAxis;
    }

    public boolean isShowLegend() {
        return this.showLegend;
    }

    public boolean isShowDataLabels() {
        return this.showDataLabels;
    }

    @Override
    public void addAggregators(SearchRequestBuilder searchRequest) {
        BucketAggregator bucketAggregator = getXAxis().getBucketAggregator().clone();
        bucketAggregator.addAggregators(getYAxis().getAggregators().stream().map(Aggregator::clone).collect(Collectors.toList()));
        searchRequest.addAggregation(bucketAggregator.toAggregationBuilder());
    }

    @Override
    public void appendHighchartsConfig(JsonBuilder builder) {
        builder.startObject("legend").field("enabled", isShowLegend()).endObject();
        builder.startObject("chart").field("type", "pie").endObject();
        builder.startObject("plotOptions").startObject("pie").startObject("dataLabels").field("enabled", isShowDataLabels()).endObject();
        builder.field("showInLegend", true);
        builder.field("allowPointSelect", true);
        if ("semi_circle".equals(getSubType())) {
            builder.field("startAngle", -90);
            builder.field("endAngle", 90);
            builder.field("innerSize", "50%");
            builder.field("center", "50%", "100%");
            builder.field("size", "100%");
        } else if ("donut".equals(getSubType())) {
            builder.field("innerSize", "50%");
            builder.field("center", "50%", "100%");
        }
        builder.endObject().endObject();
    }

    @Override
    public String getValueFormat() {
        return getYAxis().getFormat();
    }

    @Override
    public void mergeFromColumn(PieGraph graph) {
        this.subType = graph.getSubType();
        this.showLegend = graph.isShowLegend();
        this.showDataLabels = graph.isShowDataLabels();
    }

    @Override
    public void prepareForSearch(DataRepository dataRepository, SearchRequestBuilder searchRequestBuilder) {
        getXAxis().getBucketAggregator().prepareForSearch(dataRepository, searchRequestBuilder);
        getYAxis().getAggregators().stream().filter(p -> p instanceof BucketAggregator).forEach(c -> ((BucketAggregator) c).prepareForSearch(dataRepository, searchRequestBuilder));
    }
}
