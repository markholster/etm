package com.jecstar.etm.core;


public abstract class TelemetryEvent {

	/**
	 * The unique ID of the event.
	 */
	public String id;
	
	/**
	 * The ID of the event this event is correlated to. This is mainly used match a response to a certain request.
	 */
	public String correlationId;

}
