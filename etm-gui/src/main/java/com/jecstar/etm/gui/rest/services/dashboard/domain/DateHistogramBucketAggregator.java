package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.server.core.converter.JsonField;

import java.util.function.Supplier;

public class DateHistogramBucketAggregator extends BucketAggregator {


    public static final String TYPE = "date_histogram";

    private static final String INTERVAL = "interval";

    @JsonField(INTERVAL)
    private String interval;

    private DateInterval dateInterval;

    public String getInterval() {
        return this.interval;
    }

    public DateInterval getDateInterval(Supplier<DateInterval> autoRangeSupplier) {
        if (this.dateInterval == null) {
            if ("auto".equals(getInterval())) {
                this.dateInterval = autoRangeSupplier.get();
            } else {
                this.dateInterval = DateInterval.safeValueOf(getInterval());
            }
        }
        return this.dateInterval;
    }

}
