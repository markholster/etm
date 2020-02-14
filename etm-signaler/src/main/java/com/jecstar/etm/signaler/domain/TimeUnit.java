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
