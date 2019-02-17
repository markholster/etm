package com.jecstar.etm.server.core.elasticsearch.domain.converter;

import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.CommonStats;

public class CommonStatsConverter extends NestedObjectConverter<CommonStats> {

    public CommonStatsConverter() {
        super(f -> new CommonStats());
    }
}
