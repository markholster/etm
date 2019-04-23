package com.jecstar.etm.server.core.domain.audit;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.domain.audit.converter.CorrelatedEventsFieldsConverter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The audit log that occurs when a user requests the content of an event.
 *
 * @author Mark Holster
 */
public class GetEventAuditLog extends AuditLog {

    public static final String CORRELATED_EVENTS = "correlated_events";
    public static final String EVENT_ID = "event_id";
    public static final String PAYLOAD_VISIBLE = "payload_visible";
    public static final String DOWNLOADED = "downloaded";

    /**
     * The id of the event that is requested.
     */
    @JsonField(EVENT_ID)
    public String eventId;

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
    public final Set<String> correlatedEvents = new HashSet<>();

    /**
     * Whether or not the payload was visible for the user.
     */
    @JsonField(PAYLOAD_VISIBLE)
    public boolean payloadVisible;

    /**
     * Whether or not the event was downloaded by the user.
     */
    @JsonField(DOWNLOADED)
    public boolean downloaded;

}
