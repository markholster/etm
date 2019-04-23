package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.common.unit.TimeValue;

public class SearchScrollRequestBuilder extends AbstractActionRequestBuilder<SearchScrollRequest> {

    private final SearchScrollRequest request;

    public SearchScrollRequestBuilder(String scrollId) {
        this.request = new SearchScrollRequest(scrollId);
    }

    public SearchScrollRequestBuilder setScroll(TimeValue timeValue) {
        this.request.scroll(timeValue);
        return this;
    }

    @Override
    public SearchScrollRequest build() {
        return this.request;
    }
}
