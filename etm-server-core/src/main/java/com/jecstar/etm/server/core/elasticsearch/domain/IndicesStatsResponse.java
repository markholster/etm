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
