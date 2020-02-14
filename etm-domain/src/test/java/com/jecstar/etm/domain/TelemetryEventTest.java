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

package com.jecstar.etm.domain;

import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.builder.ApplicationBuilder;
import com.jecstar.etm.domain.builder.EndpointBuilder;
import com.jecstar.etm.domain.builder.EndpointHandlerBuilder;
import com.jecstar.etm.domain.builder.MessagingTelemetryEventBuilder;
import com.jecstar.etm.domain.writer.json.MessagingTelemetryEventWriterJsonImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Test class for the <code>TelemetryEvent</code> class.
 *
 * @author mark
 */
public class TelemetryEventTest {

    @Test
    public void testGetCalculatedHash() {
        MessagingTelemetryEventBuilder builder1 = new MessagingTelemetryEventBuilder();
        MessagingTelemetryEventBuilder builder2 = new MessagingTelemetryEventBuilder();

        builder1.setId("Test ID");
        builder2.setId("Test ID");
        assertEquals(builder1.build().getCalculatedHash(), builder2.build().getCalculatedHash());
        builder2.setId("Wrong");
        assertNotEquals(builder1.build().getCalculatedHash(), builder2.build().getCalculatedHash());
        builder2.setId("Test ID");


        final EndpointHandlerBuilder guiEndpointHandler = new EndpointHandlerBuilder()
                .setTransactionId(UUID.randomUUID().toString())
                .setApplication(new ApplicationBuilder()
                        .setName("Gui application")
                        .setVersion("1.0.0")
                );
        final EndpointHandlerBuilder backendEndpointHandler = new EndpointHandlerBuilder()
                .setTransactionId(UUID.randomUUID().toString())
                .setApplication(new ApplicationBuilder()
                        .setName("My Backend")
                        .setVersion("2.1.0_beta3")
                );

        // A user requests the shopping card page from our public http site.
        Instant timestamp = Instant.now();

        builder1.setPayload("<shoppingcard_request><customer_id>543214</customer_id></shoppingcard_request>")
                .setPayloadFormat(PayloadFormat.XML)
                .setMessagingEventType(MessagingEventType.REQUEST)
                .setExpiry(timestamp.plusSeconds(30))
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("BACKEND.QUEUE.1")
                        .addEndpointHandler(guiEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                        .addEndpointHandler(backendEndpointHandler.setType(EndpointHandler.EndpointHandlerType.READER))
                )
                .build();

        builder2.setPayload("<shoppingcard_request><customer_id>543214</customer_id></shoppingcard_request>")
                .setPayloadFormat(PayloadFormat.XML)
                .setMessagingEventType(MessagingEventType.REQUEST)
                .setExpiry(timestamp.plusSeconds(30))
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("BACKEND.QUEUE.1")
                        .addEndpointHandler(guiEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                        .addEndpointHandler(backendEndpointHandler.setType(EndpointHandler.EndpointHandlerType.READER))
                )
                .build();

        assertEquals(builder1.build().getCalculatedHash(), builder2.build().getCalculatedHash());
        MessagingTelemetryEventWriterJsonImpl writerJson = new MessagingTelemetryEventWriterJsonImpl();
        System.out.println(writerJson.write(builder1.build()));
    }

}
