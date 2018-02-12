package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

import java.text.Format;

public class LongAggregationKey implements AggregationKey {

    private final Long key;
    private final Format format;

    public LongAggregationKey(Long key, Format format) {
        this.key = key;
        this.format = format;
    }

    @Override
    public int compareTo(AggregationKey o) {
        if (o instanceof LongAggregationKey) {
            LongAggregationKey other = (LongAggregationKey) o;
            return this.key.compareTo(other.key);
        }
        return 0;
    }

    @Override
    public String getKeyAsString() {
        return this.format.format(key);
    }

    @Override
    public int getLength() {
        return getKeyAsString().length();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LongAggregationKey) {
            LongAggregationKey other = (LongAggregationKey) obj;
            return this.key.equals(other.key);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }
}
