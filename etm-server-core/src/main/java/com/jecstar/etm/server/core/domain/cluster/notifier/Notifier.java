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

package com.jecstar.etm.server.core.domain.cluster.notifier;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.custom.EnumConverter;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;

/**
 * A notifier that is able to send notifications to the end user or another system.
 */
public abstract class Notifier {

    public enum NotifierType {
        ETM_BUSINESS_EVENT, EMAIL, SNMP;

        public static NotifierType safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return NotifierType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public static final String NAME = "name";
    public static final String NOTIFIER_TYPE = "type";

    @JsonField(NAME)
    private String name;
    @JsonField(value = NOTIFIER_TYPE, converterClass = EnumConverter.class)
    private NotifierType notifierType;


    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Notifier.NotifierType getNotifierType() {
        return this.notifierType;
    }

    public void setNotifierType(Notifier.NotifierType notifierType) {
        this.notifierType = notifierType;
    }

    /**
     * Test the connection to the backend.
     *
     * @param dataRepository The <code>DataRepository</code> that holds the trusted server certificates.
     * @return A <code>ConnectionTestResult</code> instance with the status and an optional error message.
     */
    public abstract ConnectionTestResult testConnection(DataRepository dataRepository);


}
