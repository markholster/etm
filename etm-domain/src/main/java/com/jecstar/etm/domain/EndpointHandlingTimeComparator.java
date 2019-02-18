package com.jecstar.etm.domain;

import java.util.Comparator;

public class EndpointHandlingTimeComparator implements Comparator<Endpoint> {

    @Override
    public int compare(Endpoint e1, Endpoint e2) {
        return e1.getEarliestReadTime().compareTo(e2.getEarliestHandlingTime());
    }

}
