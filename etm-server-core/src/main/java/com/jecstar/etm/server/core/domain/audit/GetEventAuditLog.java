package com.jecstar.etm.server.core.domain.audit;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.audit.converter.CorrelatedEventsFieldsConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * The audit log that occurs when a user requests the content of an event.
 *
 * @author Mark Holster
 */
public class GetEventAuditLog extends AuditLog {

    public static final String CORRELATED_EVENTS = "correlated_events";
    public static final String EVENT_ID = "event_id";
    public static final String EVENT_TYPE = "event_type";

    /**
     * The id of the event that is requested.
     */
    @JsonField(EVENT_ID)
    public String eventId;

    /**
     * The event type that is requested.
     */
    @JsonField(EVENT_TYPE)
    public String eventType;

    /**
     * A boolean that tells if the event is found or not.
     */
    @JsonField("found")
    public boolean found;

    /**
     * The name of the event that is requested.
     */
    @JsonField("event_name")
    public String eventName;

    /**
     * The map that contains the correlated events returned with the main event. The key will be the eventId and the value the eventType.
     */
    @JsonField(value = CORRELATED_EVENTS, converterClass = CorrelatedEventsFieldsConverter.class)
    public final Map<String, String> correlatedEvents = new HashMap<>();

}
