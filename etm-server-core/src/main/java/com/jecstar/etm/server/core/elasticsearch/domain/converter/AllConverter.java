package com.jecstar.etm.server.core.elasticsearch.domain.converter;

import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.IndicesStatsResponse;

public class AllConverter extends NestedObjectConverter<IndicesStatsResponse.All> {

    public AllConverter() {
        super(f -> new IndicesStatsResponse.All());
    }
}
