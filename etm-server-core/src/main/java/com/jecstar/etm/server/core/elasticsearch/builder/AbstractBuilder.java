package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.ActionRequest;

abstract class AbstractBuilder<T extends ActionRequest> {

    public abstract T build();

}
