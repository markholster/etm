package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.server.core.converter.JsonField;

public abstract class BucketAggregator {

    public static final String AGGREGATOR_TYPE = "aggregator";
    private static final String FIELD = "field";
    private static final String FIELD_TYPE = "field_type";


    @JsonField(AGGREGATOR_TYPE)
    private String aggregatorType;
    @JsonField(FIELD)
    private String field;
    @JsonField(FIELD_TYPE)
    private String fieldType;

    public String getAggregatorType() {
        return this.aggregatorType;
    }

    public String getField() {
        return this.field;
    }

    public String getFieldType() {
        return this.fieldType;
    }

}
