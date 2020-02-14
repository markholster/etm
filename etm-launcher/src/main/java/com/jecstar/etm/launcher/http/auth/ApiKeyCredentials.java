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

package com.jecstar.etm.launcher.http.auth;

import io.undertow.security.idm.Credential;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ApiKeyCredentials implements Credential {

    private final List<String> apiKeys;

    public ApiKeyCredentials(String apiKeys) {
        this.apiKeys = Arrays.stream(apiKeys.split(",")).collect(Collectors.toList());
    }

    public List<String> getApiKeys() {
        return this.apiKeys;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApiKeyCredentials)) return false;
        ApiKeyCredentials that = (ApiKeyCredentials) o;
        return Objects.equals(apiKeys, that.apiKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiKeys);
    }

}
