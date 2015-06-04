package com.jecstar.etm.core;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


public abstract class TelemetryEvent {

	/**
	 * The unique ID of the event.
	 */
	public String id;
	
	/**
	 * The ID of the event this event is correlated to. This is mainly used match a response to a certain request.
	 */
	public String correlationId;
	
	/**
	 * Data to be used for correlating event's that aren't correlated by the correlation id.
	 */
	public Map<String, String> correlationData = new HashMap<String, String>();
	
	
	/**
	 * Gives the time the event occurred.
	 * 
	 * @return The time the event occurred.
	 */
	public abstract LocalDateTime getEventTime();

}
