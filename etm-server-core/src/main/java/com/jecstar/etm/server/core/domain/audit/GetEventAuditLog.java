package com.jecstar.etm.server.core.domain.audit;

import java.util.HashMap;
import java.util.Map;

/**
 * The audit log that occurs when a user requests the content of an event.
 * 
 * @author Mark Holster
 */
public class GetEventAuditLog extends AuditLog<GetEventAuditLog> {
	
	/**
	 * The id of the event that is requested.
	 */
	public String eventId;
	
	/**
	 * The event type that is requested.
	 */
	public String eventType;
	
	/**
	 * A boolean that tells if the event is found or not. 
	 */
	public boolean found;
	
	/**
	 * The name of the event that is requested.
	 */
	public String eventName;
	
	/**
	 * The map that contains the correlated events returned with the main event. The key will be the eventId and the value the eventType.
	 */
	public Map<String, String> correlatedEvents = new HashMap<String, String>();

}
