package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

import java.text.Format;

public class DateTimeAggregationKey implements AggregationKey {

    private final long epochtime;
    private final Format format;

    public DateTimeAggregationKey(long epochtime, Format format) {
        this.epochtime = epochtime;
        this.format = format;
    }

    @Override
    public int compareTo(AggregationKey o) {
        if (o instanceof DateTimeAggregationKey) {
            DateTimeAggregationKey other = (DateTimeAggregationKey) o;
            return Long.compare(this.epochtime, other.epochtime);
        }
        return 0;
    }

    @Override
    public String getKeyAsString() {
        return this.format.format(this.epochtime);
    }

    @Override
    public int getLength() {
        return getKeyAsString().length();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DateTimeAggregationKey) {
            DateTimeAggregationKey other = (DateTimeAggregationKey) obj;
            return this.epochtime == other.epochtime;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.epochtime);
    }
}
