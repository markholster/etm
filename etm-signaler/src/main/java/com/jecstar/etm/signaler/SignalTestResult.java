package com.jecstar.etm.signaler;

import com.jecstar.etm.server.core.domain.cluster.notifier.Notifier;
import com.jecstar.etm.signaler.backoff.AlwaysNotifyBackoffPolicy;
import com.jecstar.etm.signaler.backoff.BackoffPolicy;
import com.jecstar.etm.signaler.backoff.ExponentialBackoffPolicy;
import com.jecstar.etm.signaler.domain.Signal;
import org.joda.time.DateTime;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class SignalTestResult {

    private final AlwaysNotifyBackoffPolicy alwaysNotifyBackoffPolicy = new AlwaysNotifyBackoffPolicy();

    private boolean executed;

    private Map<DateTime, Double> thresholdExceedances = new HashMap<>();

    private int consecutiveFailures = 0;
    private Duration testInterval;

    SignalTestResult setExectued(boolean executed) {
        this.executed = executed;
        return this;
    }

    public boolean isExecuted() {
        return this.executed;
    }

    SignalTestResult addThresholdExceedance(DateTime moment, Double value) {
        this.thresholdExceedances.put(moment, value);
        return this;
    }

    Map<DateTime, Double> getThresholdExceedances() {
        return this.thresholdExceedances;
    }

    boolean isLimitExceeded(Signal signal) {
        return this.thresholdExceedances.size() >= signal.getLimit();
    }

    public SignalTestResult setConsecutiveFailures(int consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
        return this;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public BackoffPolicy getNotificationBackoffPolicy(Notifier notifier) {
        switch (notifier.getNotifierType()) {
            case EMAIL:
                return new ExponentialBackoffPolicy(getConsecutiveFailures(), getTestInterval());
            default:
                return this.alwaysNotifyBackoffPolicy;

        }
    }

    public void setTestInterval(Duration testInterval) {
        this.testInterval = testInterval;
    }

    public Duration getTestInterval() {
        return this.testInterval;
    }
}
