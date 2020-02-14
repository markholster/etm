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

package com.jecstar.etm.gui.rest.services.search;

import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;

public class DefaultSearchTemplates {

    public static final String TEMPLATE_NAME_EVENTS_OF_LAST_60_MINS = "Events of last 60 mins";
    public static final String TEMPLATE_NAME_EVENTS_OF_TODAY = "Events of today";
    public static final String TEMPLATE_NAME_EVENTS_OF_YESTERDAY = "Events of yesterday";

    public String toJson(EtmPrincipal etmPrincipal) {
        String templates = "{\"user\": { \"search_templates\":[" +
                new SearchRequestParameters("*", "now-1h", "now", etmPrincipal).toJsonSearchTemplate(TEMPLATE_NAME_EVENTS_OF_LAST_60_MINS) +
                "," + new SearchRequestParameters("*", "now/d", "now/d", etmPrincipal).toJsonSearchTemplate(TEMPLATE_NAME_EVENTS_OF_TODAY) +
                "," + new SearchRequestParameters("*", "now-1d/d", "now-1d/d", etmPrincipal).toJsonSearchTemplate(TEMPLATE_NAME_EVENTS_OF_YESTERDAY) +
                "]}}";
        return templates;
    }
}