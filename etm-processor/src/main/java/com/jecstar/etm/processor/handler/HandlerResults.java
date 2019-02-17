package com.jecstar.etm.processor.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HandlerResults {

    private List<HandlerResult> results = new ArrayList<>();

    public void addHandlerResult(HandlerResult result) {
        this.results.add(result);
    }

    public boolean hasFailures() {
        return this.results.stream().anyMatch(HandlerResult::isFailed);
    }

    public boolean hasParseFailures() {
        return this.results.stream().anyMatch(HandlerResult::hasParseFailure);
    }

    public List<HandlerResult> getFailures() {
        return this.results.stream().filter(HandlerResult::isFailed).collect(Collectors.toList());
    }
}
