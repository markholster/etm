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

package com.jecstar.etm.server.core.enhancers;

import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.builder.HttpTelemetryEventBuilder;
import com.jecstar.etm.server.core.domain.parser.CopyValueExpressionParser;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


public class DefaultTelemetryEventEnhancerTest {


    @Test
    public void testParserCopiesMetadataToId() {
        final String id = "12345";
        DefaultTelemetryEventEnhancer enhancer = new DefaultTelemetryEventEnhancer();
        DefaultField field = new DefaultField("id");
        field.setParsersSource("metadata.http_X-Request-ID");
        field.getParsers().add(new CopyValueExpressionParser("Copy the value"));
        field.setWritePolicy(DefaultField.WritePolicy.WHEN_EMPTY);
        enhancer.addField(field);

        HttpTelemetryEvent event = new HttpTelemetryEventBuilder().setHttpEventType(HttpTelemetryEvent.HttpEventType.GET).addMetadata("http_X-Request-ID", id).build();
        assertNull(event.id);
        enhancer.enhance(event, ZonedDateTime.now());
        assertEquals(id, event.id);
    }

}
