package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import java.util.Map;

public class IndexRequestBuilder extends AbstractBuilder<IndexRequest> {

    private final IndexRequest request;

    public IndexRequestBuilder() {
        this.request = new IndexRequest();
    }

    public IndexRequestBuilder(String index) {
        this.request = new IndexRequest(index);
    }

    public IndexRequestBuilder(String index, String type) {
        this.request = new IndexRequest(index, type);
    }

    public IndexRequestBuilder(String index, String type, String id) {
        this.request = new IndexRequest(index, type, id);
    }

    public IndexRequestBuilder setIndex(String index) {
        this.request.index(index);
        return this;
    }

    public IndexRequestBuilder setType(String type) {
        this.request.type(type);
        return this;
    }

    public IndexRequestBuilder setId(String id) {
        this.request.id(id);
        return this;
    }

    public IndexRequestBuilder setSource(Map<String, Object> source) {
        this.request.source(source);
        return this;
    }

    public IndexRequestBuilder setSource(String source, XContentType contentType) {
        this.request.source(source, contentType);
        return this;
    }

    public IndexRequestBuilder setWaitForActiveShards(ActiveShardCount activeShardCount) {
        this.request.waitForActiveShards(activeShardCount);
        return this;
    }

    public IndexRequestBuilder setTimeout(TimeValue timeValue) {
        this.request.timeout(timeValue);
        return this;
    }

    @Override
    public IndexRequest build() {
        return this.request;
    }
}
