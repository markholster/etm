package com.jecstar.etm.server.core.domain.aggregator.converter;

import com.jecstar.etm.server.core.converter.custom.NestedListObjectConverter;
import com.jecstar.etm.server.core.domain.aggregator.pipeline.ScriptParameter;

import java.util.List;

public class ScriptParameterListConverter extends NestedListObjectConverter<ScriptParameter, List<ScriptParameter>> {

    public ScriptParameterListConverter() {
        super(new ScriptParameterConverter());
    }
}
