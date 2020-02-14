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

package com.jecstar.etm.server.core.domain.parser;

import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;

public enum ExpressionParserField {

    ID(new TelemetryEventTagsJsonImpl().getIdTag()),
    CORRELATION_ID(new TelemetryEventTagsJsonImpl().getCorrelationIdTag()),
    NAME(new TelemetryEventTagsJsonImpl().getNameTag()),
    WRITER_TRANSACTION_ID(
            new TelemetryEventTagsJsonImpl().getEndpointsTag()
                    + "." + new TelemetryEventTagsJsonImpl().getEndpointHandlersTag() + "[" + new TelemetryEventTagsJsonImpl().getEndpointHandlerTypeTag() + "=" + EndpointHandler.EndpointHandlerType.WRITER.name() + "]"
                    + "." + new TelemetryEventTagsJsonImpl().getEndpointHandlerTransactionIdTag()),
    WRITER_METADATA(
            new TelemetryEventTagsJsonImpl().getEndpointsTag()
                    + "." + new TelemetryEventTagsJsonImpl().getEndpointHandlersTag() + "[" + new TelemetryEventTagsJsonImpl().getEndpointHandlerTypeTag() + "=" + EndpointHandler.EndpointHandlerType.WRITER.name() + "]"
                    + "." + new TelemetryEventTagsJsonImpl().getMetadataTag() + "."),
    READER_TRANSACTION_ID(
            new TelemetryEventTagsJsonImpl().getEndpointsTag()
                    + "." + new TelemetryEventTagsJsonImpl().getEndpointHandlersTag() + "[" + new TelemetryEventTagsJsonImpl().getEndpointHandlerTypeTag() + "=" + EndpointHandler.EndpointHandlerType.READER.name() + "]"
                    + "." + new TelemetryEventTagsJsonImpl().getEndpointHandlerTransactionIdTag()),
    READER_METADATA(
            new TelemetryEventTagsJsonImpl().getEndpointsTag()
                    + "." + new TelemetryEventTagsJsonImpl().getEndpointHandlersTag() + "[" + new TelemetryEventTagsJsonImpl().getEndpointHandlerTypeTag() + "=" + EndpointHandler.EndpointHandlerType.READER.name() + "]"
                    + "." + new TelemetryEventTagsJsonImpl().getMetadataTag() + "."),
    CORRELATION_DATA(new TelemetryEventTagsJsonImpl().getCorrelationDataTag() + "."),
    EXTRACTED_DATA(new TelemetryEventTagsJsonImpl().getExtractedDataTag() + "."),
    METADATA(new TelemetryEventTagsJsonImpl().getMetadataTag() + ".");

    private final String jsonTag;

    ExpressionParserField(String jsonTag) {
        this.jsonTag = jsonTag;
    }

    public String getJsonTag() {
        return this.jsonTag;
    }

    public String getCollectionKeyName(String fullKey) {
        int ix = fullKey.indexOf(getJsonTag());
        if (ix == -1) {
            return null;
        }
        return fullKey.substring(ix + getJsonTag().length());
    }
}
