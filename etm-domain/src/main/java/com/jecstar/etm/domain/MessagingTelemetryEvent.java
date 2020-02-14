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

package com.jecstar.etm.domain;

import java.time.Instant;

/**
 * A <code>TelemetryEvent</code> that describes a message received from or sent to a messaging infrastructure like ActiveMQ.
 */
public class MessagingTelemetryEvent extends TelemetryEvent<MessagingTelemetryEvent> {

    public enum MessagingEventType {

        REQUEST, RESPONSE, FIRE_FORGET;

        public static MessagingEventType safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return MessagingEventType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /**
     * The moment this event expires, in case of a request.
     */
    public Instant expiry;

    /**
     * The messaging type that this event represents.
     */
    public MessagingEventType messagingEventType;

    @Override
    public MessagingTelemetryEvent initialize() {
        super.internalInitialize();
        this.expiry = null;
        this.messagingEventType = null;
        return this;
    }

    @Override
    public MessagingTelemetryEvent initialize(MessagingTelemetryEvent copy) {
        super.internalInitialize(copy);
        this.expiry = copy.expiry;
        this.messagingEventType = copy.messagingEventType;
        return this;
    }
}
