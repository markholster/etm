package com.jecstar.etm.server.core.domain.aggregator.pipeline;

import com.jecstar.etm.server.core.converter.JsonField;
import org.elasticsearch.search.aggregations.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.pipeline.SerialDiffPipelineAggregationBuilder;

public class SerialDifferencingPipelineAggregator extends PathBasedPipelineAggregator {

    public static final String TYPE = "serial_diff";
    private static final String LAG = "lag";

    @JsonField(LAG)
    private int lag = 1;

    public SerialDifferencingPipelineAggregator() {
        super();
        setPipelineAggregatorType(TYPE);
    }

    public int getLag() {
        return this.lag;
    }

    @Override
    public SerialDifferencingPipelineAggregator clone() {
        SerialDifferencingPipelineAggregator clone = new SerialDifferencingPipelineAggregator();
        super.clone(clone);
        clone.lag = this.lag;
        return clone;
    }

    @Override
    public SerialDiffPipelineAggregationBuilder toAggregationBuilder() {
        return PipelineAggregatorBuilders.diff(getId(), getPath())
                .lag(getLag())
                .setMetaData(getMetadata());
    }
}
