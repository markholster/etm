package com.jecstar.etm.processor.jms.handler;

import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.builder.EndpointBuilder;
import com.jecstar.etm.domain.builder.EndpointHandlerBuilder;
import com.jecstar.etm.domain.builder.MessagingTelemetryEventBuilder;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.handler.HandlerResult;
import com.jecstar.etm.processor.handler.HandlerResults;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import javax.jms.JMSException;
import javax.jms.Message;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

public class ClonedMessageHandler extends AbstractJMSEventHandler {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ClonedMessageHandler.class);

	
	private final StringBuilder byteArrayBuilder = new StringBuilder();
	private final TelemetryCommandProcessor telemetryCommandProcessor;
	private final MessagingTelemetryEventBuilder messagingTelemetryEventBuilder = new MessagingTelemetryEventBuilder(); 

	
	public ClonedMessageHandler(TelemetryCommandProcessor telemetryCommandProcessor) {
		this.telemetryCommandProcessor = telemetryCommandProcessor;
	}

	@Override
	protected TelemetryCommandProcessor getProcessor() {
		return this.telemetryCommandProcessor;
	}

	public HandlerResults handleMessage(Message message) {
	    HandlerResults results = new HandlerResults();
		this.messagingTelemetryEventBuilder.initialize();
		try {
			parseMessage(message);
			this.telemetryCommandProcessor.processTelemetryEvent(this.messagingTelemetryEventBuilder);
			results.addHandlerResult(HandlerResult.processed());
		} catch (JMSException e) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Unable to process content.", e);
			}
			results.addHandlerResult(HandlerResult.parserFailure(e));
		}
		return results;
	}
	
	
	private void parseMessage(Message message) throws JMSException {
		putNonNullDataInMap("JMS_Type", message.getJMSType(), this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("JMS_DeliveryMode", "" + message.getJMSDeliveryMode(), this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("JMS_Priority", "" + message.getJMSPriority(), this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("JMS_Redelivered", "" + message.getJMSRedelivered(), this.messagingTelemetryEventBuilder.getMetadata());

		this.messagingTelemetryEventBuilder.setPayload(getContent(message));
		
		this.messagingTelemetryEventBuilder
			.setId(parseId(message.getJMSMessageID()))
			.setCorrelationId(parseId(message.getJMSCorrelationID()))
            .setMessagingEventType(MessagingEventType.FIRE_FORGET)
            .addOrMergeEndpoint(new EndpointBuilder()
				.setWritingEndpointHandler(new EndpointHandlerBuilder
					().setHandlingTime(ZonedDateTime.ofInstant(Instant.ofEpochMilli(message.getJMSTimestamp() != 0 ? message.getJMSTimestamp() : System.currentTimeMillis()), ZoneOffset.UTC))));
		if (message.getJMSExpiration() != 0) {
			this.messagingTelemetryEventBuilder.setExpiry(ZonedDateTime.ofInstant(Instant.ofEpochMilli(message.getJMSExpiration()), ZoneOffset.UTC));
		}
	}
	
	private void putNonNullDataInMap(String key, String value, Map<String, Object> map) {
		if (value != null && value.trim().length() > 0) {
			map.put(key, value.trim());
		}
	}

	private String parseId(String id) {
	    if (id == null || id.length() == 0) {
	        return null;
        }
        if (id.startsWith("ID:")) {
	        // By spec all id's should start with "ID:" but we don't trust all providers so we build in this check.
	        return id.substring(3);
        }
        return id;
    }
}
