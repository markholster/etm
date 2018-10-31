package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.server.core.converter.JsonField;

public class MetricAggregator {

    private static final String ID = "id";
    private static final String AGGREGATOR_TYPE = "aggregator";
    private static final String FIELD = "field";
    private static final String FIELD_TYPE = "field_type";
    private static final String LABEL = "label";
    private static final String PERCENTILE_DATA = "percentile_data";

    @JsonField(ID)
    private String id;
    @JsonField(AGGREGATOR_TYPE)
    private String aggregatorType;
    @JsonField(FIELD)
    private String field;
    @JsonField(FIELD_TYPE)
    private String fieldType;
    @JsonField(LABEL)
    private String label;
    @JsonField(PERCENTILE_DATA)
    private Double percentileData;

    public String getId() {
        return this.id;
    }

    public String getAggregatorType() {
        return this.aggregatorType;
    }

    public String getField() {
        return this.field;
    }

    public String getFieldType() {
        return this.fieldType;
    }

    public String getLabel() {
        return this.label;
    }

    public Double getPercentileData() {
        return this.percentileData;
    }
}
