package com.jecstar.etm.server.core.domain.aggregator.pipeline;

import com.jecstar.etm.server.core.converter.JsonField;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.pipeline.MovFnPipelineAggregationBuilder;

import java.util.HashMap;
import java.util.Map;

public class MovingFunctionAggregator extends PathBasedPipelineAggregator {

    public static final String TYPE = "moving_function";
    private static final String WINDOW = "window";
    private static final String SCRIPT = "script";
    private static final String ALPHA = "alpha";
    private static final String BETA = "beta";
    private static final String GAMMA = "gamma";
    private static final String PERIOD = "period";
    private static final String MULTIPLICATIVE = "multiplicative";

    @JsonField(WINDOW)
    private int window = 1;
    @JsonField(SCRIPT)
    private String script;
    @JsonField(ALPHA)
    private Double alpha;
    @JsonField(BETA)
    private Double beta;
    @JsonField(GAMMA)
    private Double gamma;
    @JsonField(PERIOD)
    private Integer period;
    @JsonField(MULTIPLICATIVE)
    private Boolean multiplicative;


    public MovingFunctionAggregator() {
        super();
        setPipelineAggregatorType(TYPE);
    }

    public int getWindow() {
        return this.window;
    }

    public String getScript() {
        return this.script;
    }

    public Double getAlpha() {
        return this.alpha;
    }

    public Double getBeta() {
        return this.beta;
    }

    public Double getGamma() {
        return this.gamma;
    }

    public Integer getPeriod() {
        return this.period;
    }

    public Boolean getMultiplicative() {
        return this.multiplicative;
    }

    @Override
    public MovingFunctionAggregator clone() {
        MovingFunctionAggregator clone = new MovingFunctionAggregator();
        super.clone(clone);
        clone.window = this.window;
        clone.script = this.script;
        clone.alpha = this.alpha;
        clone.beta = this.beta;
        clone.gamma = this.gamma;
        clone.period = this.period;
        clone.multiplicative = this.multiplicative;
        return clone;
    }

    @Override
    public MovFnPipelineAggregationBuilder toAggregationBuilder() {
        Map<String, Object> params = new HashMap<>();
        params.put(ALPHA, getAlpha());
        params.put(BETA, getBeta());
        params.put(GAMMA, getGamma());
        params.put(PERIOD, getPeriod());
        params.put(MULTIPLICATIVE, getMultiplicative());
        if (getPeriod() != null) {
            params.put("bootstrap_window", getPeriod() * 2);
        }
        return PipelineAggregatorBuilders.movingFunction(getId(), new Script(ScriptType.INLINE, "painless", getScript(), params), getPath(), getWindow()).setMetaData(getMetadata());
    }
}
