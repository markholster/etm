package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

public interface AggregationKey extends Comparable<AggregationKey> {

    String getKeyAsString();

    int getLength();

}
