package com.jecstar.etm.signaler.backoff;

public class AlwaysNotifyBackoffPolicy implements BackoffPolicy {
    @Override
    public boolean shouldBeNotified() {
        return true;
    }
}
