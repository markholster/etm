package com.jecstar.etm.server.core.elasticsearch.domain.converter;

import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.IndexStats;

public class IndexStatsConverter extends NestedObjectConverter<IndexStats> {
    public IndexStatsConverter() {
        super(f -> new IndexStats());
    }
}
