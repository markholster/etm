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
 * Additional information added when logging the error.
 */
public class Log {

    @JsonField("level")
    private String level;
    @JsonField("logger_name")
    private String loggerName;
    @JsonField("message")
    private String message;
    @JsonField("param_message")
    private String paramMessage;

    /**
     * The severity of the record.
     */
    public String getLevel() {
        return this.level;
    }

    /**
     * The name of the logger instance used.
     */
    public String getLoggerName() {
        return this.loggerName;
    }

    /**
     * The additionally logged error message.
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * A parametrized message. E.g. 'Could not connect to %s'. The property message is still required, and should be equal to the param_message, but with placeholders replaced. In some situations the param_message is used to group errors together. The string is not interpreted, so feel free to use whichever placeholders makes sense in the client languange.
     */
    public String getParamMessage() {
        return this.paramMessage;
    }
}