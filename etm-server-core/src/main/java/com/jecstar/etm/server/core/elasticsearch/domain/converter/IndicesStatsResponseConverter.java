package com.jecstar.etm.server.core.elasticsearch.domain.converter;

import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.IndicesStatsResponse;

public class IndicesStatsResponseConverter extends NestedObjectConverter<IndicesStatsResponse> {
    public IndicesStatsResponseConverter() {
        super(f -> new IndicesStatsResponse());
    }
}
