/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.server.core.domain.audit;

import com.jecstar.etm.server.core.converter.JsonField;

import java.util.HashSet;
import java.util.Set;

/**
 * The audit log that occurs when a user requests the content of an event.
 *
 * @author Mark Holster
 */
public class GetEventAuditLog extends AuditLog {

    public static final String CORRELATED_EVENTS = "correlated_events";
    public static final String EVENT_ID = "event_id";
    public static final String DOWNLOADED = "downloaded";
    public static final String REDACTED_FIELDS = "redacted_fields";

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
     * The set that contains the id's of correlated events returned with the main event.
     */
    @JsonField(value = CORRELATED_EVENTS)
    public final Set<String> correlatedEvents = new HashSet<>();

    /**
     * The redacted fields.
     */
    @JsonField(value = REDACTED_FIELDS)
    public Set<String> redactedFields;

    /**
     * Whether or not the event was downloaded by the user.
     */
    @JsonField(DOWNLOADED)
    public boolean downloaded;

}
