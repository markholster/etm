package com.jecstar.etm.server.core.domain.aggregator.converter;

import com.jecstar.etm.server.core.converter.custom.NestedListObjectConverter;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;

import java.util.List;

public class AggregatorListConverter extends NestedListObjectConverter<Aggregator, List<Aggregator>> {

    public AggregatorListConverter() {
        super(new AggregatorConverter());
    }
}
