package com.jecstar.etm.processor.ws;

import java.util.Date;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.TelemetryEventType;
import com.jecstar.etm.processor.TelemetryEvent;

public class XmlTelemetryEvent {

	@XmlEnum
	public enum Direction {
		@XmlEnumValue("INCOMING")
		INCOMING, 
		@XmlEnumValue("OUTGOING")
		OUTGOING
	}
	
	@XmlEnum
	public enum Type {
		@XmlEnumValue("MESSAGE_REQUEST")
		MESSAGE_REQUEST,
		@XmlEnumValue("MESSAGE_RESPONSE")
		MESSAGE_RESPONSE, 
		@XmlEnumValue("MESSAGE_DATAGRAM")
		MESSAGE_DATAGRAM
	}
	
	@XmlElement
	public String id;
	
	@XmlElement
	public String application;
	
	@XmlElement
	public String content;
	
	@XmlElement
	public String correlationId;

	@XmlElement
	public Date creationTime;
	
	@XmlElement
	public Direction direction;

	@XmlElement
	public String endpoint;

	@XmlElement
	public Date expiryTime;

	@XmlElement
	public Map<String, String> metadata;
	
	@XmlElement
	public String name;

	@XmlElement
	public String transactionName;

	@XmlElement
	public Type type;

	public void copyToTelemetryEvent(TelemetryEvent telemetryEvent) {
	    telemetryEvent.application = this.application;
	    telemetryEvent.content = this.content;
	    if (this.creationTime != null) {
	    	telemetryEvent.creationTime.setTime(this.creationTime.getTime());
	    }
	    if (this.direction != null) {
	    	telemetryEvent.direction = TelemetryEventDirection.valueOf(this.direction.name());
	    }
	    telemetryEvent.endpoint = this.endpoint;
	    if (this.expiryTime != null) {
	    	telemetryEvent.expiryTime.setTime(this.expiryTime.getTime());
	    }
	    if (this.metadata != null) {
	    	telemetryEvent.metadata.putAll(this.metadata);
	    }
	    telemetryEvent.name = this.name;
	    telemetryEvent.correlationId = this.correlationId;
	    telemetryEvent.id = this.id;
	    telemetryEvent.transactionName = this.transactionName;
	    if (this.type != null) {
	    	telemetryEvent.type = TelemetryEventType.valueOf(this.type.name());
	    }
    }

}
