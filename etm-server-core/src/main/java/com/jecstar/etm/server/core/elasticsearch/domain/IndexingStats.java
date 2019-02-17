package com.jecstar.etm.server.core.elasticsearch.domain;

import com.jecstar.etm.server.core.converter.JsonField;
import org.elasticsearch.common.unit.TimeValue;

public class IndexingStats {

    @JsonField("index_total")
    private long indexTotal;

    @JsonField("index_time_in_millis")
    private long indexTimeInMillis;

    public long getIndexCount() {
        return this.indexTotal;
    }

    public TimeValue getIndexTime() {
        return new TimeValue(this.indexTimeInMillis);
    }
}
