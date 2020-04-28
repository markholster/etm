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

/**
 * If a log record was generated as a result of a http request, the http interface can be used to collect this information.
 */
public class Request {

    @JsonField("body")
    private String body;
    @JsonField(value = "env", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.EnvConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Env env;
    @JsonField("headers")
    private java.util.Map<String, Object> headers;
    @JsonField("http_version")
    private String httpVersion;
    @JsonField("method")
    private String method;
    @JsonField(value = "socket", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.SocketConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Socket socket;
    @JsonField(value = "url", converterClass = com.jecstar.etm.processor.elastic.apm.domain.converter.UrlConverter.class)
    private com.jecstar.etm.processor.elastic.apm.domain.Url url;

    /**
     * Data should only contain the request body (not the query string). It can either be a dictionary (for standard HTTP requests) or a raw request body.
     */
    public String getBody() {
        return this.body;
    }

    /**
     * The env variable is a compounded of environment information passed from the webserver.
     */
    public com.jecstar.etm.processor.elastic.apm.domain.Env getEnv() {
        return this.env;
    }

    /**
     * Should include any headers sent by the requester. Cookies will be taken by headers if supplied.
     */
    public java.util.Map<String, Object> getHeaders() {
        return this.headers;
    }

    /**
     * HTTP version.
     */
    public String getHttpVersion() {
        return this.httpVersion;
    }

    /**
     * HTTP method.
     */
    public String getMethod() {
        return this.method;
    }

    public com.jecstar.etm.processor.elastic.apm.domain.Socket getSocket() {
        return this.socket;
    }

    /**
     * A complete Url, with scheme, host and path.
     */
    public com.jecstar.etm.processor.elastic.apm.domain.Url getUrl() {
        return this.url;
    }
}