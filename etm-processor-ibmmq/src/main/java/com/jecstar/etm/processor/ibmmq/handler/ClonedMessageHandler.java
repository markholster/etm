package com.jecstar.etm.processor.ibmmq.handler;

import com.ibm.mq.MQMessage;
import com.ibm.mq.constants.CMQC;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.builder.EndpointBuilder;
import com.jecstar.etm.domain.builder.EndpointHandlerBuilder;
import com.jecstar.etm.domain.builder.MessagingTelemetryEventBuilder;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.handler.HandlerResult;
import com.jecstar.etm.processor.handler.HandlerResults;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

public class ClonedMessageHandler extends AbstractMQEventHandler {
	
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

	public HandlerResults handleMessage(MQMessage message) {
		HandlerResults results = new HandlerResults();
		this.messagingTelemetryEventBuilder.initialize();
		try {
			parseMessage(message);
			this.telemetryCommandProcessor.processTelemetryEvent(this.messagingTelemetryEventBuilder);
			results.addHandlerResult(HandlerResult.processed());
		} catch (IOException e) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Unable to process content.", e);
			}
			results.addHandlerResult(HandlerResult.parserFailure(e));
		}
		return results;
	}
	
	
	private void parseMessage(MQMessage message) throws IOException {
		putNonNullDataInMap("MQMD_CharacterSet", "" + message.characterSet, this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("MQMD_Format", message.format, this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("MQMD_Encoding", "" + message.encoding, this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("MQMD_AccountingToken", message.accountingToken, this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("MQMD_Persistence", "" + message.persistence, this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("MQMD_Priority", "" + message.priority, this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("MQMD_ReplyToQueueManager", message.replyToQueueManagerName, this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("MQMD_ReplyToQueue", message.replyToQueueName, this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("MQMD_BackoutCount", "" + message.backoutCount, this.messagingTelemetryEventBuilder.getMetadata());
		
		this.messagingTelemetryEventBuilder.setPayload(getContent(message));
		
		this.messagingTelemetryEventBuilder
			.setId(byteArrayToString(message.messageId))
			.setCorrelationId(byteArrayToString(message.correlationId))
			.addOrMergeEndpoint(new EndpointBuilder()
					.setWritingEndpointHandler(new EndpointHandlerBuilder
							().setHandlingTime(ZonedDateTime.ofInstant(Instant.ofEpochMilli(message.putDateTime.getTimeInMillis()), ZoneOffset.UTC))));
		if (message.expiry != CMQC.MQEI_UNLIMITED) {
			this.messagingTelemetryEventBuilder.setExpiry(ZonedDateTime.ofInstant(Instant.ofEpochMilli(message.putDateTime.getTimeInMillis() + (message.expiry * 100)), ZoneOffset.UTC));
		}
		if (message.messageType == CMQC.MQMT_REQUEST) {
			this.messagingTelemetryEventBuilder.setMessagingEventType(MessagingEventType.REQUEST);
		} else if (message.messageType == CMQC.MQMT_REPLY) {
			this.messagingTelemetryEventBuilder.setMessagingEventType(MessagingEventType.RESPONSE);
		} else if (message.messageType == CMQC.MQMT_DATAGRAM) {
			this.messagingTelemetryEventBuilder.setMessagingEventType(MessagingEventType.FIRE_FORGET);
		}
	}
	
	private void putNonNullDataInMap(String key, String value, Map<String, Object> map) {
		if (value != null && value.trim().length() > 0) {
			map.put(key, value.trim());
		}
	}

	private void putNonNullDataInMap(String key, byte[] value, Map<String, Object> map) {
		if (value != null && value.length > 0) {
			putNonNullDataInMap(key, byteArrayToString(value), map);
		}
	}
	
	private String byteArrayToString(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		this.byteArrayBuilder.setLength(0);
		boolean allZero = true;
		for (byte aByte : bytes) {
			this.byteArrayBuilder.append(String.format("%02x", aByte));
			if (aByte != 0) {
				allZero = false;
			}
		}
		return allZero ? null : this.byteArrayBuilder.toString();
	}
}
