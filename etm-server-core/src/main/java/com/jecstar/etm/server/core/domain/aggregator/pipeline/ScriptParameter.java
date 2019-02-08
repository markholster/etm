package com.jecstar.etm.server.core.domain.aggregator.pipeline;

import com.jecstar.etm.server.core.converter.JsonField;

public class ScriptParameter implements Cloneable {

    private static final String NAME = "name";
    private static final String AGGREGATOR_ID = "aggregator_id";

    @JsonField(NAME)
    private String name;
    @JsonField(AGGREGATOR_ID)
    private String aggregatorId;

    public String getName() {
        return this.name;
    }

    public String getAggregatorId() {
        return this.aggregatorId;
    }

    @Override
    public ScriptParameter clone() {
        ScriptParameter clone = new ScriptParameter();
        clone.name = this.name;
        clone.aggregatorId = this.aggregatorId;
        return clone;
    }
}
