package com.jecstar.etm.server.core.domain.cluster.notifier;

public class ConnectionTestResult {

    public static final ConnectionTestResult OK = new ConnectionTestResult();

    private static byte SUCCESS = 0;
    private static byte FAILED = 1;

    private final byte status;
    private final String errorMessage;

    private ConnectionTestResult() {
        this.status = SUCCESS;
        this.errorMessage = null;
    }

    public ConnectionTestResult(String errorMessage) {
        this.status = FAILED;
        this.errorMessage = errorMessage;
    }

    public boolean isFailed() {
        return this.status == FAILED;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }
}
