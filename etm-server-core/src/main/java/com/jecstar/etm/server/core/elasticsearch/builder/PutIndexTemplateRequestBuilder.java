package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;

import java.util.List;

public class PutIndexTemplateRequestBuilder extends AbstractActionRequestBuilder<PutIndexTemplateRequest> {

    private final PutIndexTemplateRequest request;

    public PutIndexTemplateRequestBuilder(String name) {
        this.request = new PutIndexTemplateRequest(name);
    }

    public PutIndexTemplateRequestBuilder setCreate(boolean create) {
        this.request.create(create);
        return this;
    }

    public PutIndexTemplateRequestBuilder setPatterns(List<String> indexPatterns) {
        this.request.patterns(indexPatterns);
        return this;
    }

    public PutIndexTemplateRequestBuilder setSettings(Settings.Builder settingsBuilder) {
        this.request.settings(settingsBuilder);
        return this;
    }

    public PutIndexTemplateRequestBuilder setMapping(String source, XContentType contentType) {
        this.request.mapping(source, contentType);
        return this;
    }

    public PutIndexTemplateRequestBuilder setAlias(Alias alias) {
        this.request.alias(alias);
        return this;
    }

    @Override
    public PutIndexTemplateRequest build() {
        return this.request;
    }
}
