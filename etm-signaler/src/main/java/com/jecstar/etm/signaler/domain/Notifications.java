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
import com.jecstar.etm.server.core.converter.custom.EnumConverter;

import java.util.List;

public class Notifications {

    public static final String INTERVAL = "interval";
    public static final String INTERVAL_TIMEUNIT = "interval_timeunit";
    public static final String MAX_FREQUENCY_OF_EXCEEDANCE = "max_frequency_of_exceedance";
    public static final String NOTIFIERS = "notifiers";
    public static final String EMAIL_RECIPIENTS = "email_recipients";
    public static final String EMAIL_ALL_ETM_GROUP_MEMBERS = "email_all_etm_group_members";


    @JsonField(INTERVAL)
    private int interval;
    @JsonField(value = INTERVAL_TIMEUNIT, converterClass = EnumConverter.class)
    private TimeUnit intervalUnit;
    @JsonField(MAX_FREQUENCY_OF_EXCEEDANCE)
    private int maxFrequencyOfExceedance;
    @JsonField(NOTIFIERS)
    private List<String> notifiers;
    @JsonField(EMAIL_RECIPIENTS)
    private List<String> emailRecipients;
    @JsonField(EMAIL_ALL_ETM_GROUP_MEMBERS)
    private Boolean emailAllEtmGroupMembers;

    public int getMaxFrequencyOfExceedance() {
        return this.maxFrequencyOfExceedance;
    }

    public int getInterval() {
        return this.interval;
    }

    public TimeUnit getIntervalUnit() {
        return this.intervalUnit;
    }

    public List<String> getNotifiers() {
        return this.notifiers;
    }

    public List<String> getEmailRecipients() {
        return this.emailRecipients;
    }

    public Boolean getEmailAllEtmGroupMembers() {
        return this.emailAllEtmGroupMembers;
    }

    public Notifications setMaxFrequencyOfExceedance(int maxFrequencyOfExceedance) {
        this.maxFrequencyOfExceedance = maxFrequencyOfExceedance;
        return this;
    }

    public Notifications setInterval(int interval) {
        this.interval = interval;
        return this;
    }

    public Notifications setIntervalUnit(TimeUnit intervalUnit) {
        this.intervalUnit = intervalUnit;
        return this;
    }

    public Notifications setNotifiers(List<String> notifiers) {
        this.notifiers = notifiers;
        return this;
    }

    public Notifications setEmailRecipients(List<String> emailRecipients) {
        this.emailRecipients = emailRecipients;
        return this;
    }

    public Notifications setEmailAllEtmGroupMembers(Boolean emailAllEtmGroupMembers) {
        this.emailAllEtmGroupMembers = emailAllEtmGroupMembers;
        return this;
    }
}
