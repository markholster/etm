package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.server.core.converter.JsonField;

public class TermBucketAggregator extends BucketAggregator {


    public static final String TYPE = "term";

    private static final String TOP = "top";
    private static final String ORDER = "order";
    private static final String ORDER_BY = "order_by";

    @JsonField(TOP)
    private Integer top;
    @JsonField(ORDER)
    private String order;
    @JsonField(ORDER_BY)
    private String orderBy;

    public Integer getTop() {
        return this.top != null ? this.top : 5;
    }

    public String getOrder() {
        return this.order;
    }

    public String getOrderBy() {
        return this.orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }
}
