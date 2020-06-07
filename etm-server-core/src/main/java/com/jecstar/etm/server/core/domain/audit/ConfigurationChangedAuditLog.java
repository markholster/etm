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

package com.jecstar.etm.server.core.domain.audit;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.custom.EnumConverter;

/**
 * Audit logs that occurs when a user changes a configuration item.
 */
public class ConfigurationChangedAuditLog extends AuditLog {

    public enum Action {CREATE, UPDATE, DELETE}

    public static final String CONFIGURATION_TYPE = "configuration_type";
    public static final String CONFIGURATION_ID = "configuration_id";
    public static final String ACTION = "action";
    public static final String OLD_VALUE = "old_value";
    public static final String NEW_VALUE = "new_value";

    /**
     * The configuration type that is changed. In most cases it will be a values that's present
     * in {@link com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout}.
     */
    @JsonField(CONFIGURATION_TYPE)
    public String configurationType;

    /**
     * The Elasticsearch id of the item that is changed.
     */
    @JsonField(CONFIGURATION_ID)
    public String configurationId;

    /**
     * The action that is performed on the configuration item.
     */
    @JsonField(value = ACTION, converterClass = EnumConverter.class)
    public Action action;

    /**
     * The value the configuration item had before the change.
     */
    @JsonField(OLD_VALUE)
    public String oldValue;

    /**
     * The value the configuration has after the change.
     */
    @JsonField(NEW_VALUE)
    public String newValue;
}
