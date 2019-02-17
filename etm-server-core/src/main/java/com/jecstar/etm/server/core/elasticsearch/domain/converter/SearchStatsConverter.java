package com.jecstar.etm.server.core.elasticsearch.domain.converter;

import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.SearchStats;

public class SearchStatsConverter extends NestedObjectConverter<SearchStats> {

    public SearchStatsConverter() {
        super(f -> new SearchStats());
    }
}
