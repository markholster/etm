package com.jecstar.etm.server.core.elasticsearch.domain.converter;

import com.jecstar.etm.server.core.converter.custom.NestedNamedMapObjectConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.IndexStats;

import java.util.Map;

public class IndicesMapConverter extends NestedNamedMapObjectConverter<IndexStats, Map<String, IndexStats>> {
    public IndicesMapConverter() {
        super(new IndexStatsConverter());
    }
}
