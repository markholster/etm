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

package com.jecstar.etm.server.core.domain.converter.json;

import com.jecstar.etm.server.core.domain.converter.ImportProfileTags;

public class ImportProfileTagsJsonImpl implements ImportProfileTags {

    @Override
    public String getEnhancerTag() {
        return "enhancer";
    }

    @Override
    public String getNameTag() {
        return "name";
    }

    @Override
    public String getEnhancerTypeTag() {
        return "type";
    }

    @Override
    public String getEnhancePayloadFormatTag() {
        return "enhance_payload_format";
    }

    @Override
    public String getFieldsTag() {
        return "fields";
    }

    @Override
    public String getWritePolicyTag() {
        return "write_policy";
    }

    @Override
    public String getParsersSourceTag() {
        return "parsers_source";
    }

    @Override
    public String getFieldTag() {
        return "field";
    }

    @Override
    public String getParsersTag() {
        return "parsers";
    }

    @Override
    public String getParserTag() {
        return "parser";
    }

    @Override
    public String getReplaceAllTag() {
        return "replace_all";
    }

    @Override
    public String getReplacementTag() {
        return "replacement";
    }

    @Override
    public String getTransformationsTag() {
        return "transformations";
    }
}
