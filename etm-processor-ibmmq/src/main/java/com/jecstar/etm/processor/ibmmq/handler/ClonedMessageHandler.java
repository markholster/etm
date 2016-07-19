package com.jecstar.etm.processor.ibmmq.handler;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import com.ibm.mq.MQMessage;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.headers.Charsets;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.builders.EndpointBuilder;
import com.jecstar.etm.domain.builders.EndpointHandlerBuilder;
import com.jecstar.etm.domain.builders.MessagingTelemetryEventBuilder;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

public class ClonedMessageHandler {
	
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
	public HandlerResult handleMessage(MQMessage message, byte[] content) {
		this.messagingTelemetryEventBuilder.initialize();
		try {
			parseMessage(message, content);
			this.telemetryCommandProcessor.processTelemetryEvent(this.messagingTelemetryEventBuilder);
			return HandlerResult.PROCESSED;
		} catch (UnsupportedEncodingException e) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Unable to process content.", e);
			}			
			return HandlerResult.PARSE_FAILURE;
		}
	}
	
	
	private void parseMessage(MQMessage message, byte[] content) throws UnsupportedEncodingException {
		putNonNullDataInMap("MQMD_CharacterSet", "" + message.characterSet, this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("MQMD_Format", message.format, this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("MQMD_Encoding", "" + message.encoding, this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("MQMD_AccountingToken", message.accountingToken, this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("MQMD_Persistence", "" + message.persistence, this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("MQMD_Priority", "" + message.priority, this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("MQMD_ReplyToQueueManager", message.replyToQueueManagerName, this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("MQMD_ReplyToQueue", message.replyToQueueName, this.messagingTelemetryEventBuilder.getMetadata());
		putNonNullDataInMap("MQMD_BackoutCount", "" + message.backoutCount, this.messagingTelemetryEventBuilder.getMetadata());
		
		this.messagingTelemetryEventBuilder.setPayload(Charsets.convert(content, message.characterSet));
		
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
		for (int i = 0; i < bytes.length; i++) {
			this.byteArrayBuilder.append(String.format("%02x", bytes[i]));
			if (bytes[i] != 0) {
				allZero = false;
			}
		}
		return allZero ? null : this.byteArrayBuilder.toString();
	}
}
