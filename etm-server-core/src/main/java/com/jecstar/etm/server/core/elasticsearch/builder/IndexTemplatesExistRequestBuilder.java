package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.client.indices.IndexTemplatesExistRequest;

public class IndexTemplatesExistRequestBuilder {

    private IndexTemplatesExistRequest request;

    public IndexTemplatesExistRequestBuilder(String indices) {
        request = new IndexTemplatesExistRequest(indices);
    }

    public IndexTemplatesExistRequest build() {
        return this.request;
    }
}
