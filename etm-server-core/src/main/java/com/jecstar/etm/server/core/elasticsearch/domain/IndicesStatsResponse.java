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

package com.jecstar.etm.server.core.elasticsearch.domain;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.elasticsearch.domain.converter.AllConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.converter.CommonStatsConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.converter.IndicesMapConverter;

import java.util.Map;

public class IndicesStatsResponse {

    @JsonField(value = "_all", converterClass = AllConverter.class)
    private All all;

    @JsonField(value = "indices", converterClass = IndicesMapConverter.class)
    private Map<String, IndexStats> indices;

    public CommonStats getTotal() {
        return this.all.total;
    }

    public CommonStats getPrimaries() {
        return this.all.primaries;
    }

    public Map<String, IndexStats> getIndices() {
        return this.indices;
    }

    public static class All {
        @JsonField(value = "total", converterClass = CommonStatsConverter.class)
        private CommonStats total;
        @JsonField(value = "primaries", converterClass = CommonStatsConverter.class)
        private CommonStats primaries;
    }
}
