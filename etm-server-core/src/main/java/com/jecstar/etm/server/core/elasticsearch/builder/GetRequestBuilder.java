package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;

public class GetRequestBuilder extends AbstractActionRequestBuilder<GetRequest> {

    private final GetRequest request;

    public GetRequestBuilder(String index, String id) {
        this.request = new GetRequest(index, id);
    }

    public GetRequestBuilder setFetchSource(boolean fetch) {
        this.request.fetchSourceContext(new FetchSourceContext(fetch));
        return this;
    }

    public GetRequestBuilder setFetchSource(String include, String exclude) {
        this.request.fetchSourceContext(new FetchSourceContext(true, new String[]{include}, new String[]{exclude}));
        return this;
    }

    public GetRequestBuilder setFetchSource(String[] includes, String[] excludes) {
        this.request.fetchSourceContext(new FetchSourceContext(true, includes, excludes));
        return this;
    }

    @Override
    public GetRequest build() {
        return this.request;
    }
}
