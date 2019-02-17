package com.jecstar.etm.processor.handler;

public class HandlerResult {

    enum Status {PROCESSED, PARSE_FAILURE, FAILED;}

    private Status status;

    private Exception exception;
    private String mesage;

    public boolean isFailed() {
        return !Status.PROCESSED.equals(this.status);
    }

    public boolean hasParseFailure() {
        return Status.PARSE_FAILURE.equals(this.status);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Status: " + this.status.name());
        if (this.mesage != null) {
            result.append(", message: " + this.mesage);
        }
        if (this.exception != null) {
            result.append(", exception:" + this.exception);
        }
        return result.toString();
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

    public static HandlerResult failed(String message) {
        HandlerResult result = new HandlerResult();
        result.status = Status.FAILED;
        result.mesage = message;
        return result;
    }
}
