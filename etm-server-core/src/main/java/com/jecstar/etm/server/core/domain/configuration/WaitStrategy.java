package com.jecstar.etm.server.core.domain.configuration;

public enum WaitStrategy {

    BLOCKING, BUSY_SPIN, SLEEPING, YIELDING;

    public static WaitStrategy safeValueOf(String value) {
        if (value == null) {
            return null;
        }
        try {
            return WaitStrategy.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
