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

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import com.jecstar.etm.server.core.domain.aggregator.converter.AggregatorListConverter;

import java.util.List;

public class YAxis {

    private static final String TITLE = "title";
    private static final String FORMAT = "format";
    private static final String AGGREGATORS = "aggregators";

    @JsonField(TITLE)
    private String title;
    @JsonField(FORMAT)
    private String format;
    @JsonField(value = AGGREGATORS, converterClass = AggregatorListConverter.class)
    private List<Aggregator> aggregators;


    public String getTitle() {
        return this.title;
    }

    public String getFormat() {
        return this.format;
    }

    public List<Aggregator> getAggregators() {
        return this.aggregators;
    }

    public YAxis setTitle(String title) {
        this.title = title;
        return this;
    }

    public YAxis setFormat(String format) {
        this.format = format;
        return this;
    }

    public YAxis setAggregators(List<Aggregator> aggregators) {
        this.aggregators = aggregators;
        return this;
    }
}
