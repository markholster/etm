package com.jecstar.etm.server.core.elasticsearch.builder;


import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;

public class DeleteIndexTemplateRequestBuilder extends AbstractActionRequestBuilder<DeleteIndexTemplateRequest> {

    private final DeleteIndexTemplateRequest request = new DeleteIndexTemplateRequest();

    public DeleteIndexTemplateRequestBuilder setName(String name) {
        this.request.name(name);
        return this;
    }

    public DeleteIndexTemplateRequest build() {
        return this.request;
    }
}
