package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;

import java.util.List;

public class PutIndexTemplateRequestBuilder extends AbstractBuilder<PutIndexTemplateRequest> {

    private final PutIndexTemplateRequest request = new PutIndexTemplateRequest();

    public PutIndexTemplateRequestBuilder setName(String name) {
        this.request.name(name);
        return this;
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

    public PutIndexTemplateRequestBuilder setMapping(String type, String source, XContentType contentType) {
        this.request.mapping(type, source, contentType);
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
