package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.ActionRequest;

abstract class AbstractActionRequestBuilder<T extends ActionRequest> {

    public abstract T build();

}
