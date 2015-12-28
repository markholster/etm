package com.jecstar.etm.processor.ibmmq.handlers;

import java.util.Map;

import com.ibm.mq.MQMessage;
import com.ibm.mq.constants.CMQC;
import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.TelemetryEventType;

public class ClonedMessageHandler implements MessageHandler<byte[]>{
	
	private final StringBuilder byteArrayBuilder = new StringBuilder();

	@Override
	public boolean handleMessage(TelemetryEvent telemetryEvent, byte[] message) {
		telemetryEvent.content = new String(message);
		return true;
	}
	
	public void handleHeader(TelemetryEvent telemetryEvent, MQMessage message) {
		putNonNullDataInMap("MQMD_CharacterSet", "" + message.characterSet, telemetryEvent.metadata);
		putNonNullDataInMap("MQMD_Format", message.format, telemetryEvent.metadata);
		putNonNullDataInMap("MQMD_Encoding", "" + message.encoding, telemetryEvent.metadata);
		putNonNullDataInMap("MQMD_AccountingToken", message.accountingToken, telemetryEvent.metadata);
		putNonNullDataInMap("MQMD_Persistence", "" + message.persistence, telemetryEvent.metadata);
		putNonNullDataInMap("MQMD_Priority", "" + message.priority, telemetryEvent.metadata);
		putNonNullDataInMap("MQMD_ReplyToQueueManager", message.replyToQueueManagerName, telemetryEvent.metadata);
		putNonNullDataInMap("MQMD_ReplyToQueue", message.replyToQueueName, telemetryEvent.metadata);
		putNonNullDataInMap("MQMD_BackoutCount", "" + message.backoutCount, telemetryEvent.metadata);
		
		telemetryEvent.id = byteArrayToString(message.messageId);
		telemetryEvent.correlationId = byteArrayToString(message.correlationId);
		telemetryEvent.creationTime.setTime(message.putDateTime.getTimeInMillis());
		if (message.expiry != CMQC.MQEI_UNLIMITED) {
			telemetryEvent.expiryTime.setTime(message.putDateTime.getTimeInMillis() + (message.expiry * 100));
		}
		if (message.messageType == CMQC.MQMT_REQUEST) {
			telemetryEvent.type = TelemetryEventType.MESSAGE_REQUEST;
		} else if (message.messageType == CMQC.MQMT_REPLY) {
			telemetryEvent.type = TelemetryEventType.MESSAGE_RESPONSE;
		} else if (message.messageType == CMQC.MQMT_DATAGRAM) {
			telemetryEvent.type = TelemetryEventType.MESSAGE_DATAGRAM;
		}

	}
	
	private void putNonNullDataInMap(String key, String value, Map<String, String> map) {
		if (value != null && value.trim().length() > 0) {
			map.put(key, value.trim());
		}
	}

	private void putNonNullDataInMap(String key, byte[] value, Map<String, String> map) {
		if (value != null && value.length > 0) {
			putNonNullDataInMap(key, byteArrayToString(value), map);
		}
	}
	
	private String byteArrayToString(byte[] bytes) {
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
