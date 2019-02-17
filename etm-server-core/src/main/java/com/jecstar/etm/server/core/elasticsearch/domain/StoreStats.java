package com.jecstar.etm.server.core.elasticsearch.domain;

import com.jecstar.etm.server.core.converter.JsonField;

public class StoreStats {

    @JsonField("size_in_bytes")
    private long sizeInBytes;

    public long getSizeInBytes() {
        return this.sizeInBytes;
    }
}
