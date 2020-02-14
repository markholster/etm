/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

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
