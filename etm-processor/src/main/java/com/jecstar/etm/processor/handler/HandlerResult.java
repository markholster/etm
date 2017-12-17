package com.jecstar.etm.processor.handler;

public class HandlerResult {

    enum Status {PROCESSED, PARSE_FAILURE, FAILED}

    private Status status;

    private Exception exception;

    public boolean isFailed() {
        return !Status.PROCESSED.equals(this.status);
    }

    public boolean hasParseFailure() {
        return Status.PARSE_FAILURE.equals(this.status);
    }

    public static HandlerResult processed() {
        HandlerResult result = new HandlerResult();
        result.status = Status.PROCESSED;
        return result;
    }

    public static HandlerResult parserFailure(Exception exception) {
        HandlerResult result = new HandlerResult();
        result.status = Status.PARSE_FAILURE;
        result.exception = exception;
        return result;
    }

    public static HandlerResult failed() {
        HandlerResult result = new HandlerResult();
        result.status = Status.FAILED;
        return result;
    }
}
