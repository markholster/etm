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

public enum PayloadEncoding {

    BASE64,
    BASE64_CA_API_GATEWAY;

    public static PayloadEncoding safeValueOf(String value) {
        if (value == null) {
            return null;
        }
        if ("base64CaApiGateway".equalsIgnoreCase(value)) {
            // Support for an old value that wasn't in this ENUM when published.
            return BASE64_CA_API_GATEWAY;
        }
        try {
            return PayloadEncoding.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
