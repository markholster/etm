package com.jecstar.etm.server.core.elasticsearch.domain;

import com.jecstar.etm.server.core.converter.JsonField;

public class DocsStats {

    @JsonField("count")
    private long count;

    @JsonField("deleted")
    private long deleted;

    public long getCount() {
        return this.count;
    }

    public long getDeleted() {
        return this.deleted;
    }
}
