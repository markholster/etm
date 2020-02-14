/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.signaler;

import com.jecstar.etm.server.core.domain.cluster.notifier.Notifier;
import com.jecstar.etm.signaler.backoff.AlwaysNotifyBackoffPolicy;
import com.jecstar.etm.signaler.backoff.BackoffPolicy;
import com.jecstar.etm.signaler.backoff.ExponentialBackoffPolicy;
import com.jecstar.etm.signaler.domain.Signal;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class SignalTestResult {

    private final AlwaysNotifyBackoffPolicy alwaysNotifyBackoffPolicy = new AlwaysNotifyBackoffPolicy();

    private boolean executed;

    private Map<ZonedDateTime, Double> thresholdExceedances = new HashMap<>();

    private int consecutiveFailures = 0;
    private Duration testInterval;

    SignalTestResult setExectued(boolean executed) {
        this.executed = executed;
        return this;
    }

    public boolean isExecuted() {
        return this.executed;
    }

    SignalTestResult addThresholdExceedance(ZonedDateTime moment, Double value) {
        this.thresholdExceedances.put(moment, value);
        return this;
    }

    Map<ZonedDateTime, Double> getThresholdExceedances() {
        return this.thresholdExceedances;
    }

    boolean isLimitExceeded(Signal signal) {
        return this.thresholdExceedances.size() >= signal.getNotifications().getMaxFrequencyOfExceedance();
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
