package com.jecstar.etm.gui.rest.services.dashboard.domain.converter;

import com.jecstar.etm.gui.rest.services.dashboard.domain.*;
import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

public class BucketAggregatorConverter extends NestedObjectConverter<BucketAggregator> {

    private final LogWrapper log = LogFactory.getLogger(MetricAggregatorConverter.class);

    private final JsonConverter jsonConverter = new JsonConverter();

    public BucketAggregatorConverter() {
        super(f -> {
            switch ((String) f.get(BucketAggregator.AGGREGATOR_TYPE)) {
                case DateHistogramBucketAggregator.TYPE:
                    return new DateHistogramBucketAggregator();
                case HistogramBucketAggregator.TYPE:
                    return new HistogramBucketAggregator();
                case SignificantTermBucketAggregator.TYPE:
                    return new SignificantTermBucketAggregator();
                case TermBucketAggregator.TYPE:
                    return new TermBucketAggregator();
                default:
                    throw new IllegalArgumentException((String) f.get(Graph.TYPE));

            }
        });
    }
}
