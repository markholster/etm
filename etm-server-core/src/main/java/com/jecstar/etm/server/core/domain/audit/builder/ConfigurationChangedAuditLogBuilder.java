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

package com.jecstar.etm.server.core.domain.audit.builder;

import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.domain.audit.ConfigurationChangedAuditLog;

public class ConfigurationChangedAuditLogBuilder extends AbstractAuditLogBuilder<ConfigurationChangedAuditLog, ConfigurationChangedAuditLogBuilder> {

    private final JsonEntityConverter<Object> entityConverter = new JsonEntityConverter<>(null);

    public ConfigurationChangedAuditLogBuilder() {
        super(new ConfigurationChangedAuditLog());
    }

    public ConfigurationChangedAuditLogBuilder setConfigurationType(String configurationType) {
        this.audit.configurationType = configurationType;
        return this;
    }

    public ConfigurationChangedAuditLogBuilder setConfigurationId(String configurationId) {
        this.audit.configurationId = configurationId;
        return this;
    }

    public ConfigurationChangedAuditLogBuilder setAction(ConfigurationChangedAuditLog.Action action) {
        this.audit.action = action;
        return this;
    }

    public ConfigurationChangedAuditLogBuilder setOldValue(String oldValue) {
        this.audit.oldValue = oldValue;
        return this;
    }

    public ConfigurationChangedAuditLogBuilder setOldValue(Object oldValue) {
        this.audit.oldValue = this.entityConverter.write(oldValue);
        return this;
    }

    public ConfigurationChangedAuditLogBuilder setNewValue(String newValue) {
        this.audit.newValue = newValue;
        return this;
    }

    public ConfigurationChangedAuditLogBuilder setNewValue(Object newValue) {
        this.audit.newValue = this.entityConverter.write(newValue);
        return this;
    }

}