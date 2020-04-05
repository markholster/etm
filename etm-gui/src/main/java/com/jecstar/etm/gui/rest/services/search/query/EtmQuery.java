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

package com.jecstar.etm.gui.rest.services.search.query;

import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.gui.rest.services.search.query.converter.AdditionalQueryParameterListConverter;
import com.jecstar.etm.gui.rest.services.search.query.converter.ResultLayoutConverter;
import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.util.DateUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class EtmQuery {

    public static final String NAME = "name";
    public static final String RESULT_LAYOUT = "result_layout";
    public static final String ADDITIONAL_QUERY_PARAMETERS = "additional_query_parameters";

    @JsonField(NAME)
    private String name;
    @JsonField(value = RESULT_LAYOUT, converterClass = ResultLayoutConverter.class)
    private ResultLayout resultLayout;
    @JsonField(value = ADDITIONAL_QUERY_PARAMETERS, converterClass = AdditionalQueryParameterListConverter.class)
    private List<AdditionalQueryParameter> additionalQueryParameters;

    public EtmQuery() {
        this.resultLayout = new ResultLayout();
        this.additionalQueryParameters = new ArrayList<>();
    }

    public EtmQuery(String query, String startTime, String endTime, EtmPrincipal etmPrincipal) {
        this();
        var tags = new TelemetryEventTagsJsonImpl();
        this.resultLayout
                .setQuery(query)
                .setStartIndex(0)
                .setMaxResults(50)
                .setSortField(tags.getTimestampTag())
                .setSortOrder(ResultLayout.SortOrder.DESC);

        this.resultLayout.addField(new Field()
                        .setName("Timestamp")
                        .setField(tags.getTimestampTag())
                        .setArraySelector(Field.ArraySelector.LOWEST)
                        .setFormat(Field.Format.ISOTIMESTAMP)
                        .setLink(true),
                etmPrincipal
        );
        this.resultLayout.addField(new Field()
                        .setName("Name")
                        .setField(tags.getNameTag())
                        .setArraySelector(Field.ArraySelector.FIRST)
                        .setFormat(Field.Format.PLAIN)
                        .setLink(false),
                etmPrincipal
        );
        if (startTime != null) {
            AdditionalQueryParameter param = new AdditionalQueryParameter()
                    .setField(tags.getTimestampTag())
                    .setFieldType(AdditionalQueryParameter.FieldType.RANGE_START);
            Instant instant = DateUtils.parseDateString(startTime, etmPrincipal.getTimeZone().toZoneId(), true);
            if (instant != null) {
                param.setValue(instant.toEpochMilli());
            } else {
                param.setValue(startTime);
            }
            this.additionalQueryParameters.add(param);
        }
        if (endTime != null) {
            AdditionalQueryParameter param = new AdditionalQueryParameter()
                    .setField(tags.getTimestampTag())
                    .setFieldType(AdditionalQueryParameter.FieldType.RANGE_END);
            Instant instant = DateUtils.parseDateString(startTime, etmPrincipal.getTimeZone().toZoneId(), true);
            if (instant != null) {
                param.setValue(instant.toEpochMilli());
            } else {
                param.setValue(startTime);
            }
            this.additionalQueryParameters.add(param);
        }
    }

    public String getName() {
        return this.name;
    }

    public EtmQuery setName(String name) {
        this.name = name;
        return this;
    }

    public ResultLayout getResultLayout() {
        return this.resultLayout;
    }

    public List<AdditionalQueryParameter> getAdditionalQueryParameters() {
        return this.additionalQueryParameters;
    }
}
