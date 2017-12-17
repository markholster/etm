package com.jecstar.etm.processor.handler;

import java.util.ArrayList;
import java.util.List;

public class HandlerResults {

    private List<HandlerResult> results = new ArrayList<>();

    public void addHandlerResult(HandlerResult result) {
        this.results.add(result);
    }

    public boolean hasFailures() {
        return this.results.stream().anyMatch(p -> p.isFailed());
    }

    public boolean hasParseFailures() {
        return this.results.stream().anyMatch(p -> p.hasParseFailure());
    }
}
