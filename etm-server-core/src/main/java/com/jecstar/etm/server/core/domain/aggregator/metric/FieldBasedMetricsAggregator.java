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

package com.jecstar.etm.server.core.domain.aggregator.metric;

import com.jecstar.etm.server.core.converter.JsonField;

public abstract class FieldBasedMetricsAggregator extends MetricsAggregator {

    private static final String FIELD = "field";

    @JsonField(FIELD)
    private String field;

    public String getField() {
        return this.field;
    }

    public FieldBasedMetricsAggregator setField(String field) {
        this.field = field;
        return this;
    }

    @Override
    public FieldBasedMetricsAggregator clone() {
        throw new UnsupportedOperationException("Clone method must be implemented in '" + getClass().getName() + ".");
    }

    protected void clone(FieldBasedMetricsAggregator target) {
        super.clone(target);
        target.field = this.field;
    }
}
