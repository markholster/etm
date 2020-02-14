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

public class ScriptParameter implements Cloneable {

    private static final String NAME = "name";
    private static final String AGGREGATOR_ID = "aggregator_id";

    @JsonField(NAME)
    private String name;
    @JsonField(AGGREGATOR_ID)
    private String aggregatorId;

    public String getName() {
        return this.name;
    }

    public String getAggregatorId() {
        return this.aggregatorId;
    }

    @Override
    public ScriptParameter clone() {
        ScriptParameter clone = new ScriptParameter();
        clone.name = this.name;
        clone.aggregatorId = this.aggregatorId;
        return clone;
    }
}
