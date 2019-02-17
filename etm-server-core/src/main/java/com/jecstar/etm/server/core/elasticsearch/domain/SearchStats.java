package com.jecstar.etm.server.core.elasticsearch.domain;

import com.jecstar.etm.server.core.converter.JsonField;

public class SearchStats {

    @JsonField("query_total")
    private long queryCount;
    @JsonField("query_time_in_millis")
    private long queryTimeInMillis;

    public long getQueryCount() {
        return this.queryCount;
    }

    public long getQueryTimeInMillis() {
        return this.queryTimeInMillis;
    }
}
