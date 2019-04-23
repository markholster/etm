package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.client.Validatable;

public abstract class AbstractTimedRequestBuilder<T extends Validatable> {

    public abstract T build();

}
