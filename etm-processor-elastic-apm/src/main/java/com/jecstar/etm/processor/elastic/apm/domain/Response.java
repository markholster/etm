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

package com.jecstar.etm.processor.elastic.apm.domain;

import com.jecstar.etm.server.core.converter.JsonField;

public class Response {

    @JsonField("finished")
    private Boolean finished;
    @JsonField("headers")
    private java.util.Map<String, Object> headers;
    @JsonField("headers_sent")
    private Boolean headersSent;
    @JsonField("status_code")
    private Long statusCode;

    /**
     * A boolean indicating whether the response was finished or not
     */
    public Boolean isFinished() {
        return this.finished;
    }

    /**
     * A mapping of HTTP headers of the response object
     */
    public java.util.Map<String, Object> getHeaders() {
        return this.headers;
    }

    public Boolean isHeadersSent() {
        return this.headersSent;
    }

    /**
     * The HTTP status code of the response.
     */
    public Long getStatusCode() {
        return this.statusCode;
    }
}