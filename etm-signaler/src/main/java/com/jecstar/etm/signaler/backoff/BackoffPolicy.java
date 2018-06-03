package com.jecstar.etm.signaler.backoff;

public interface BackoffPolicy {

    boolean shouldBeNotified();
}
