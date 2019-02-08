package com.jecstar.etm.server.core.domain.aggregator.converter;

import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.domain.aggregator.pipeline.ScriptParameter;

public class ScriptParameterConverter extends JsonEntityConverter<ScriptParameter> {

    public ScriptParameterConverter() {
        super(f -> new ScriptParameter());
    }
}
