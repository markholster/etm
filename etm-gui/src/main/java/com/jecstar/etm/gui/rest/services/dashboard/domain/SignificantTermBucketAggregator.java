package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.server.core.converter.JsonField;

public class SignificantTermBucketAggregator extends BucketAggregator {


    public static final String TYPE = "significant_term";

    private static final String TOP = "top";

    @JsonField(TOP)
    private Integer top;

    public Integer getTop() {
        return this.top != null ? this.top : 5;
    }

}
