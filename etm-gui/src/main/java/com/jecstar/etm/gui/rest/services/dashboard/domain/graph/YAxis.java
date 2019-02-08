package com.jecstar.etm.gui.rest.services.dashboard.domain.graph;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import com.jecstar.etm.server.core.domain.aggregator.converter.AggregatorListConverter;

import java.util.List;

public class YAxis {

    private static final String TITLE = "title";
    private static final String FORMAT = "format";
    private static final String AGGREGATORS = "aggregators";

    @JsonField(TITLE)
    private String title;
    @JsonField(FORMAT)
    private String format;
    @JsonField(value = AGGREGATORS, converterClass = AggregatorListConverter.class)
    private List<Aggregator> aggregators;


    public String getTitle() {
        return this.title;
    }

    public String getFormat() {
        return this.format;
    }

    public List<Aggregator> getAggregators() {
        return this.aggregators;
    }

    public YAxis setTitle(String title) {
        this.title = title;
        return this;
    }

    public YAxis setFormat(String format) {
        this.format = format;
        return this;
    }

    public YAxis setAggregators(List<Aggregator> aggregators) {
        this.aggregators = aggregators;
        return this;
    }
}
