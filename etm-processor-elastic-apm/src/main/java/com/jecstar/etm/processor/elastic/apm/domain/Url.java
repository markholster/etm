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
 * A complete Url, with scheme, host and path.
 */
public class Url {

    @JsonField("raw")
    private String raw;
    @JsonField("protocol")
    private String protocol;
    @JsonField("full")
    private String full;
    @JsonField("hostname")
    private String hostname;
    @JsonField("port")
    private String port;
    @JsonField("pathname")
    private String pathname;
    @JsonField("search")
    private String search;
    @JsonField("hash")
    private String hash;

    /**
     * The raw, unparsed URL of the HTTP request line, e.g https://example.com:443/search?q=elasticsearch. This URL may be absolute or relative. For more details, see https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.1.2.
     */
    public String getRaw() {
        return this.raw;
    }

    /**
     * The protocol of the request, e.g. 'https:'.
     */
    public String getProtocol() {
        return this.protocol;
    }

    /**
     * The full, possibly agent-assembled URL of the request, e.g https://example.com:443/search?q=elasticsearch#top.
     */
    public String getFull() {
        return this.full;
    }

    /**
     * The hostname of the request, e.g. 'example.com'.
     */
    public String getHostname() {
        return this.hostname;
    }

    /**
     * The port of the request, e.g. '443'
     */
    public String getPort() {
        return this.port;
    }

    /**
     * The path of the request, e.g. '/search'
     */
    public String getPathname() {
        return this.pathname;
    }

    /**
     * The search describes the query string of the request. It is expected to have values delimited by ampersands.
     */
    public String getSearch() {
        return this.search;
    }

    /**
     * The hash of the request URL, e.g. 'top'
     */
    public String getHash() {
        return this.hash;
    }
}