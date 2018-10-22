package com.jecstar.etm.domain;

import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.builder.ApplicationBuilder;
import com.jecstar.etm.domain.builder.EndpointBuilder;
import com.jecstar.etm.domain.builder.EndpointHandlerBuilder;
import com.jecstar.etm.domain.builder.MessagingTelemetryEventBuilder;
import org.junit.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
    }

}
