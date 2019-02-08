package com.jecstar.etm.signaler.domain;

import java.time.Duration;

public enum TimeUnit {
    MINUTES() {
        @Override
        public Duration toDuration(int duration) {
            return Duration.ofMinutes(duration);
        }

        @Override
        public String toTimestampExpression(int amount) {
            return amount + "m";
        }
    }, HOURS() {
        @Override
        public Duration toDuration(int duration) {
            return Duration.ofHours(duration);
        }

        @Override
        public String toTimestampExpression(int amount) {
            return amount + "h";
        }
    }, DAYS() {
        @Override
        public Duration toDuration(int duration) {
            return Duration.ofDays(duration);
        }

        @Override
        public String toTimestampExpression(int amount) {
            return amount + "d";
        }
    };

    public static TimeUnit safeValueOf(String value) {
        if (value == null) {
            return null;
        }
        try {
            return TimeUnit.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public abstract Duration toDuration(int duration);

    public abstract String toTimestampExpression(int amount);
}
