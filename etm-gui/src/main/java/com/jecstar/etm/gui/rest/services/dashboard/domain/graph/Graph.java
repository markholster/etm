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
import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;

public abstract class Graph<T extends Graph> {

    public static final String TYPE = "type";

    @JsonField(TYPE)
    private String type;

    public String getType() {
        return this.type;
    }

    protected Graph<T> setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Add the aggregators to the <code>SearchRequestBuilder</code>
     *
     * @param searchRequest The <code>SearchRequestBuilder</code> instance on which the aggregators should be added.
     */
    public abstract void addAggregators(SearchRequestBuilder searchRequest);

    /**
     * Append the highcharts configuration (as json) to a <code>JsonBuilder</code>.
     *
     * @param builder The <code>JsonBuilder</code>
     */
    public abstract void appendHighchartsConfig(JsonBuilder builder);

    /**
     * Gives the D3 format of the values in the graph.
     *
     * @return The format of the values in the graph.6
     */
    public abstract String getValueFormat();

    /**
     * Merge an other <code>Graph</code> instance configured at a <code>Dashboard</code>'s <code>Column</code>.
     *
     * @param graph The <code>Graph</code> to merge from.
     */
    public abstract void mergeFromColumn(T graph);

    /**
     * Prepares the <code>Graph</code> for the search to be executed. This gives <code>Aggregator</code>s the option
     * to configure themselves in the context of the search to be executed. As an example <code>Aggregator</code>s
     * with an automatic range can determine their range by inspecting the range configured in the query.
     *
     * @param dataRepository       The <code>DataRepository</code>.
     * @param searchRequestBuilder The <code>SearchRequestBuilder</code> to be executed in the next step.
     */
    public abstract void prepareForSearch(DataRepository dataRepository, SearchRequestBuilder searchRequestBuilder);
}
