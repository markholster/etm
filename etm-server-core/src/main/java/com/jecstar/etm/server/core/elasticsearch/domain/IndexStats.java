package com.jecstar.etm.server.core.elasticsearch.domain;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.elasticsearch.domain.converter.CommonStatsConverter;

public class IndexStats {

    @JsonField(value = "total", converterClass = CommonStatsConverter.class)
    private CommonStats total;
    @JsonField(value = "primaries", converterClass = CommonStatsConverter.class)
    private CommonStats primaries;

    public CommonStats getTotal() {
        return this.total;
    }

    public CommonStats getPrimaries() {
        return this.primaries;
    }
}
