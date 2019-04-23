package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;

public class GetAliasesRequestBuilder extends AbstractActionRequestBuilder<GetAliasesRequest> {

    private final GetAliasesRequest request = new GetAliasesRequest();

    public GetAliasesRequestBuilder setIndices(String indices) {
        this.request.indices(indices);
        return this;
    }

    public GetAliasesRequestBuilder setAliases(String aliases) {
        this.request.aliases(aliases);
        return this;
    }

    @Override
    public GetAliasesRequest build() {
        return this.request;
    }
}
