package com.jecstar.etm.processor.mdb.ibm;

import com.jecstar.etm.core.TelemetryEventType;

public enum NodeType {
	// See https://www-01.ibm.com/support/knowledgecenter/SSMKHH_9.0.0/com.ibm.etools.mft.doc/as36001_.htm
	ComIbmMQInputNode(null),
	ComIbmMQOutputNode(null),
	ComIbmMQGetNode(null),
	
	ComIbmJMSClientInputNode(null),
	ComIbmJMSClientOutputNode(null),
	ComIbmJMSClientReceive(null),
	ComIbmJMSClientReplyNode(TelemetryEventType.MESSAGE_RESPONSE),
	
	ComIbmHTTPAsyncRequest(TelemetryEventType.MESSAGE_REQUEST),
	ComIbmHTTPAsyncResponse(TelemetryEventType.MESSAGE_RESPONSE),
	ComIbmWSInputNode(TelemetryEventType.MESSAGE_REQUEST),
	ComIbmWSReplyNode(TelemetryEventType.MESSAGE_RESPONSE),
	ComIbmWSRequestNode(TelemetryEventType.MESSAGE_REQUEST),
	
	ComIbmSOAPInputNode(TelemetryEventType.MESSAGE_REQUEST),
	ComIbmSOAPReplyNode(TelemetryEventType.MESSAGE_RESPONSE),
	ComIbmSOAPRequestNode(TelemetryEventType.MESSAGE_REQUEST),
	ComIbmSOAPAsyncRequestNode(TelemetryEventType.MESSAGE_REQUEST),
	ComIbmSOAPAsyncResponseNode(TelemetryEventType.MESSAGE_RESPONSE);	

	private final TelemetryEventType eventType;

	private NodeType(TelemetryEventType eventType) {
		this.eventType = eventType;
	}
	
	public TelemetryEventType getEventType() {
		return this.eventType;
	}

	public static NodeType nullSafeValueOf(String nodeType) {
		if (nodeType == null) {
			return null;
		}
		try {
			return NodeType.valueOf(nodeType);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
}
