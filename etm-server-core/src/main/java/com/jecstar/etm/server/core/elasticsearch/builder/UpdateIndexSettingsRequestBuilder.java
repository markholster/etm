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

package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.common.settings.Settings;

public class UpdateIndexSettingsRequestBuilder extends AbstractActionRequestBuilder<UpdateSettingsRequest> {

    private final UpdateSettingsRequest request = new UpdateSettingsRequest();

    public UpdateIndexSettingsRequestBuilder setIndices(String... indices) {
        this.request.indices(indices);
        return this;
    }

    public UpdateIndexSettingsRequestBuilder setSettings(Settings.Builder settingsBuilder) {
        this.request.settings(settingsBuilder);
        return this;
    }

    @Override
    public UpdateSettingsRequest build() {
        return this.request;
    }
}
