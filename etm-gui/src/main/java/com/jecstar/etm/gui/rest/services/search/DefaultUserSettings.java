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

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.gui.rest.services.search.query.AdditionalQueryParameter;
import com.jecstar.etm.gui.rest.services.search.query.EtmQuery;
import com.jecstar.etm.gui.rest.services.search.query.converter.AdditionalQueryParameterConverter;
import com.jecstar.etm.gui.rest.services.search.query.converter.EtmQueryConverter;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;

public class DefaultUserSettings {

    public static final String TEMPLATE_NAME_EVENTS_OF_LAST_60_MINS = "Events of last 60 mins";
    public static final String TEMPLATE_NAME_EVENTS_OF_TODAY = "Events of today";
    public static final String TEMPLATE_NAME_EVENTS_OF_YESTERDAY = "Events of yesterday";
    private final EtmQueryConverter queryConverter = new EtmQueryConverter();
    private final AdditionalQueryParameterConverter paramConverter = new AdditionalQueryParameterConverter();

    public String toJson(EtmPrincipal etmPrincipal, int maxSearchTemplates) {
        JsonBuilder builder = new JsonBuilder();
        builder.startObject().startObject("user");
        builder.startArray("search_templates");
        if (maxSearchTemplates >= 1) {
            builder.rawElement(this.queryConverter.write(
                    new EtmQuery("*", "now-1h", "now", etmPrincipal).setName(TEMPLATE_NAME_EVENTS_OF_LAST_60_MINS)
            ));
        }
        if (maxSearchTemplates >= 2) {
            builder.rawElement(this.queryConverter.write(
                    new EtmQuery("*", "now/d", "now/d", etmPrincipal).setName(TEMPLATE_NAME_EVENTS_OF_TODAY)
            ));
        }
        if (maxSearchTemplates >= 3) {
            builder.rawElement(this.queryConverter.write(
                    new EtmQuery("*", "now-1d/d", "now-1d/d", etmPrincipal).setName(TEMPLATE_NAME_EVENTS_OF_YESTERDAY)
            ));
        }
        builder.endArray();

        builder.startArray("additional_query_parameters");
        builder.rawElement(this.paramConverter.write(
                new AdditionalQueryParameter()
                        .setName("Start date")
                        .setField("timestamp")
                        .setFieldType(AdditionalQueryParameter.FieldType.RANGE_START)
                        .setDefaultValue("now-1h")

        ));
        builder.rawElement(this.paramConverter.write(
                new AdditionalQueryParameter()
                        .setName("End date")
                        .setField("timestamp")
                        .setFieldType(AdditionalQueryParameter.FieldType.RANGE_END)
                        .setDefaultValue("now")

        ));
        builder.endArray();
        builder.endObject().endObject();
        return builder.build();
    }
}