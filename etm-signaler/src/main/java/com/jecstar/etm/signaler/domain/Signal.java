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

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.util.DateUtils;
import com.jecstar.etm.signaler.domain.converter.DataConverter;
import com.jecstar.etm.signaler.domain.converter.NotificationsConverter;
import com.jecstar.etm.signaler.domain.converter.ThresholdConverter;

import java.time.Instant;

public class Signal {

    public static final String NAME = "name";
    public static final String ENABLED = "enabled";
    public static final String DATA = "data";
    public static final String THRESHOLD = "threshold";
    public static final String NOTIFICATIONS = "notifications";

    @JsonField(NAME)
    private String name;
    @JsonField(ENABLED)
    private boolean enabled = true;
    @JsonField(value = DATA, converterClass = DataConverter.class)
    private Data data;
    @JsonField(value = THRESHOLD, converterClass = ThresholdConverter.class)
    private Threshold threshold;
    @JsonField(value = NOTIFICATIONS, converterClass = NotificationsConverter.class)
    private Notifications notifications;

    // Fields used by this signaler, but not stored in this class.
    public static final String LAST_EXECUTED = "last_executed_timestamp";
    public static final String LAST_FAILED = "last_failed_timestamp";
    public static final String FAILED_SINCE = "failed_since_timestamp";
    public static final String LAST_PASSED = "last_passed_timestamp";

    /**
     * An array of keys that aren't stored by the SignalConverter but need to be copied when the Signal is updated.
     */
    public static String[] METADATA_KEYS = new String[]{
            LAST_EXECUTED, LAST_FAILED, FAILED_SINCE, LAST_PASSED
    };

    public String getName() {
        return this.name;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public Data getData() {
        return this.data;
    }

    public Threshold getThreshold() {
        return this.threshold;
    }

    public Notifications getNotifications() {
        return this.notifications;
    }

    public Signal setName(String name) {
        this.name = name;
        return this;
    }

    public Signal setData(Data data) {
        this.data = data;
        return this;
    }

    public Signal setThreshold(Threshold threshold) {
        this.threshold = threshold;
        return this;
    }

    public Signal setNotifications(Notifications notifications) {
        this.notifications = notifications;
        return this;
    }

    public void normalizeQueryTimesToInstant(EtmPrincipal etmPrincipal) {
        if (this.data != null) {
            if (this.data.getFrom() != null) {
                Instant instant = DateUtils.parseDateString(this.data.getFrom(), etmPrincipal.getTimeZone().toZoneId(), true);
                if (instant != null) {
                    this.data.setFrom("" + instant.toEpochMilli());
                }
            }
            if (this.data.getTill() != null) {
                Instant instant = DateUtils.parseDateString(this.data.getTill(), etmPrincipal.getTimeZone().toZoneId(), false);
                if (instant != null) {
                    this.data.setTill("" + instant.toEpochMilli());
                }
            }
        }
    }
}
