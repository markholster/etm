package com.jecstar.etm.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Test;

import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.builder.ApplicationBuilder;
import com.jecstar.etm.domain.builder.EndpointBuilder;
import com.jecstar.etm.domain.builder.EndpointHandlerBuilder;
import com.jecstar.etm.domain.builder.MessagingTelemetryEventBuilder;

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
		ZonedDateTime timestamp = ZonedDateTime.now();

		builder1.setPayload("<shoppingcard_request><customer_id>543214</customer_id></shoppingcard_request>")
		.setPayloadFormat(PayloadFormat.XML)
		.setMessagingEventType(MessagingEventType.REQUEST)
		.setExpiry(timestamp.plusSeconds(30))
		.addOrMergeEndpoint(new EndpointBuilder()
					.setName("BACKEND.QUEUE.1")
					.setWritingEndpointHandler(guiEndpointHandler)
					.addReadingEndpointHandler(backendEndpointHandler)
				)
		.build();
		
		builder2.setPayload("<shoppingcard_request><customer_id>543214</customer_id></shoppingcard_request>")
		.setPayloadFormat(PayloadFormat.XML)
		.setMessagingEventType(MessagingEventType.REQUEST)
		.setExpiry(timestamp.plusSeconds(30))
		.addOrMergeEndpoint(new EndpointBuilder()
					.setName("BACKEND.QUEUE.1")
					.setWritingEndpointHandler(guiEndpointHandler)
					.addReadingEndpointHandler(backendEndpointHandler)
				)
		.build();
		
		assertEquals(builder1.build().getCalculatedHash(), builder2.build().getCalculatedHash());
	}

}
