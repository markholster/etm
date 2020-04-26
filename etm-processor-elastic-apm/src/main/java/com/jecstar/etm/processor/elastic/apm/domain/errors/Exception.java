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

package com.jecstar.etm.processor.elastic.apm.domain.errors;

import com.jecstar.etm.server.core.converter.JsonField;

/**
 * Information about the originally thrown error.
 */
public class Exception {

    @JsonField("code")
    private String code;
    @JsonField("message")
    private String message;
    @JsonField("module")
    private String module;
    @JsonField("type")
    private String type;
    @JsonField("handled")
    private Boolean handled;

    /**
     * The error code set when the error happened, e.g. database error code.
     */
    public String getCode() {
        return this.code;
    }

    /**
     * The original error message.
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Describes the exception type's module namespace.
     */
    public String getModule() {
        return this.module;
    }

    public String getType() {
        return this.type;
    }

    /**
     * Indicator whether the error was caught somewhere in the code or not.
     */
    public Boolean isHandled() {
        return this.handled;
    }
}