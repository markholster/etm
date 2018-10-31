package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.server.core.converter.JsonField;

public class HistogramBucketAggregator extends BucketAggregator {


    public static final String TYPE = "histogram";

    private static final String INTERVAL = "interval";

    @JsonField(INTERVAL)
    private Double interval;

    public Double getInterval() {
        return this.interval != null ? this.interval : 1D;
    }

}
