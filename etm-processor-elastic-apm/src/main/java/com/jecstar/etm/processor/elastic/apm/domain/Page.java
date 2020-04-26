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

public class Page {

    @JsonField("referer")
    private String referer;
    @JsonField("url")
    private String url;

    /**
     * RUM specific field that stores the URL of the page that 'linked' to the current page.
     */
    public String getReferer() {
        return this.referer;
    }

    /**
     * RUM specific field that stores the URL of the current page
     */
    public String getUrl() {
        return this.url;
    }
}