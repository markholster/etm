package com.jecstar.etm.server.core.elasticsearch.domain.converter;

import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.DocsStats;

public class DocsStatsConverter extends NestedObjectConverter<DocsStats> {

    public DocsStatsConverter() {
        super(f -> new DocsStats());
    }
}
