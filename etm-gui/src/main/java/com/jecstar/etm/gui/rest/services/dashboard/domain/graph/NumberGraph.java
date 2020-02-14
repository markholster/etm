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

package com.jecstar.etm.gui.rest.services.dashboard.domain.graph;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.YAxisConverter;
import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.BucketAggregator;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.BaseAggregationBuilder;
import org.elasticsearch.search.aggregations.PipelineAggregationBuilder;

public class NumberGraph extends Graph<NumberGraph> {

    public static final String TYPE = "number";
    public static final String Y_AXIS = "y_axis";
    public static final String FONT_SIZE = "font_size";

    @JsonField(value = Y_AXIS, converterClass = YAxisConverter.class)
    private YAxis yAxis;

    @JsonField(value = FONT_SIZE)
    private float fontSize = 4;

    public NumberGraph() {
        super();
        setType(TYPE);
    }

    public YAxis getYAxis() {
        return this.yAxis;
    }

    public NumberGraph setyAxis(YAxis yAxis) {
        this.yAxis = yAxis;
        return this;
    }

    public float getFontSize() {
        return this.fontSize;
    }

    public NumberGraph setFontSize(float fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    @Override
    public void addAggregators(SearchRequestBuilder searchRequest) {
        for (Aggregator aggregator : getYAxis().getAggregators()) {
            BaseAggregationBuilder baseAggregationBuilder = aggregator.toAggregationBuilder();
            if (baseAggregationBuilder instanceof AggregationBuilder) {
                searchRequest.addAggregation((AggregationBuilder) baseAggregationBuilder);
            } else if (baseAggregationBuilder instanceof PipelineAggregationBuilder) {
                searchRequest.addAggregation((PipelineAggregationBuilder) baseAggregationBuilder);
            } else {
                throw new IllegalArgumentException("Unknown aggregation builder '" + baseAggregationBuilder.getClass().getName() + "'.");
            }
        }
    }

    @Override
    public void appendHighchartsConfig(JsonBuilder builder) {
        builder.field("font_size", getFontSize() + "em");
    }

    @Override
    public String getValueFormat() {
        return getYAxis().getFormat();
    }

    @Override
    public void mergeFromColumn(NumberGraph graph) {
        this.fontSize = graph.getFontSize();
    }

    @Override
    public void prepareForSearch(DataRepository dataRepository, SearchRequestBuilder searchRequest) {
        getYAxis().getAggregators().stream().filter(p -> p instanceof BucketAggregator).forEach(c -> ((BucketAggregator) c).prepareForSearch(dataRepository, searchRequest));
    }
}
