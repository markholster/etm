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

package com.jecstar.etm.server.core.domain.aggregator;

import com.jecstar.etm.server.core.converter.JsonField;
import org.elasticsearch.search.aggregations.BaseAggregationBuilder;

import java.util.HashMap;
import java.util.Map;

public abstract class Aggregator implements Cloneable {

    public static final String KEYWORD_SUFFIX = ".keyword";
    public static final String AGGREGATOR_TYPE = "type";
    private static final String ID = "id";
    public static final String NAME = "name";
    public static final String SHOW_ON_GRAPH = "show_on_graph";

    @JsonField(AGGREGATOR_TYPE)
    private String aggregatorType;
    @JsonField(ID)
    private String id;
    @JsonField(NAME)
    private String name;
    @JsonField(SHOW_ON_GRAPH)
    private Boolean showOnGraph;

    public String getAggregatorType() {
        return this.aggregatorType;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Boolean isShowOnGraph() {
        return this.showOnGraph;
    }

    protected Aggregator setAggregatorType(String aggregatorType) {
        this.aggregatorType = aggregatorType;
        return this;
    }

    public Aggregator setId(String id) {
        this.id = id;
        return this;
    }

    public Aggregator setName(String name) {
        this.name = name;
        return this;
    }

    public Aggregator setShowOnGraph(Boolean showOnGraph) {
        this.showOnGraph = showOnGraph;
        return this;
    }

    protected void clone(Aggregator target) {
        target.aggregatorType = this.aggregatorType;
        target.id = this.id;
        target.name = this.name;
        target.showOnGraph = this.showOnGraph;
    }

    @Override
    public Aggregator clone() {
        throw new UnsupportedOperationException("Clone method must be implemented in '" + getClass().getName() + ".");
    }

    public abstract BaseAggregationBuilder toAggregationBuilder();

    protected Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(ID, getId());
        metadata.put(NAME, getName());
        metadata.put(SHOW_ON_GRAPH, isShowOnGraph());
        return metadata;
    }
}
