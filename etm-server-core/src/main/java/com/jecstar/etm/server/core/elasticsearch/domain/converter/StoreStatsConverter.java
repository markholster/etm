package com.jecstar.etm.server.core.elasticsearch.domain.converter;

import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.StoreStats;

public class StoreStatsConverter extends NestedObjectConverter<StoreStats> {

    public StoreStatsConverter() {
        super(f -> new StoreStats());
    }
}
