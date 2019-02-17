package com.jecstar.etm.server.core.elasticsearch.domain.converter;

import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.IndexingStats;

public class IndexingStatsConverter extends NestedObjectConverter<IndexingStats> {

    public IndexingStatsConverter() {
        super(f -> new IndexingStats());
    }
}
