package com.jecstar.etm.core;

import java.util.UUID;

public class TelemetryEvent {

	/**
	 * The unique ID of the event.
	 */
	public UUID id;
	
	/**
	 * The ID of the event this event is correlated to. This is mainly used match a response to a certain request.
	 */
	public UUID correlationId;
	
	/**
	 * The ID of the source of this event, for example a JMSMessageID.
	 */
	public String sourceId;


}
