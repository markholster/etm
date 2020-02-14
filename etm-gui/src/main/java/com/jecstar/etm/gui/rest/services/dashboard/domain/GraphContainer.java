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

package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.DataConverter;
import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.GraphConverter;
import com.jecstar.etm.gui.rest.services.dashboard.domain.graph.Graph;
import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.util.DateUtils;

import java.time.Instant;

/**
 * A <code>GraphContainer</code> holds all information to display a visual representation of one ore more groups of data.
 */
public class GraphContainer {

    public static final String NAME = "name";
    public static final String DATA = "data";
    public static final String GRAPH = "graph";

    @JsonField(NAME)
    private String name;

    @JsonField(value = DATA, converterClass = DataConverter.class)
    private Data data;

    @JsonField(value = GRAPH, converterClass = GraphConverter.class)
    private Graph<Graph> graph;

    public String getName() {
        return this.name;
    }

    public Data getData() {
        return this.data;
    }

    public Graph<Graph> getGraph() {
        return this.graph;
    }

    public GraphContainer setName(String name) {
        this.name = name;
        return this;
    }

    public GraphContainer setData(Data data) {
        this.data = data;
        return this;
    }

    @SuppressWarnings("unchecked")
    public GraphContainer setGraph(Graph<? extends Graph> graph) {
        this.graph = (Graph<Graph>) graph;
        return this;
    }

    public void mergeFromColumn(Column column) {
        if (column.getData() != null) {
            getData().mergeFromColumn(column.getData());
        }
        if (column.getGraph() != null) {
            getGraph().mergeFromColumn(column.getGraph());
        }
    }

    public void normalizeQueryTimesToInstant(EtmPrincipal etmPrincipal) {
        if (this.data != null) {
            if (this.data.getFrom() != null) {
                Instant instant = DateUtils.parseDateString(this.data.getFrom(), etmPrincipal.getTimeZone().toZoneId(), true);
                if (instant != null) {
                    this.data.setFrom("" + instant.toEpochMilli());
                }
            }
            if (this.data.getTill() != null) {
                Instant instant = DateUtils.parseDateString(this.data.getTill(), etmPrincipal.getTimeZone().toZoneId(), false);
                if (instant != null) {
                    this.data.setTill("" + instant.toEpochMilli());
                }
            }
        }
    }
}
