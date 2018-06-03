package com.jecstar.etm.server.core.domain.audit;

import com.jecstar.etm.server.core.converter.JsonField;

import java.time.ZonedDateTime;

public abstract class AuditLog {

    public static final String HANDLING_TIME = "handling_time";
    public static final String TIMESTAMP = "timestamp";
    public static final String PRINCIPAL_ID = "principal_id";


    /**
     * The time the audit was written to the database.
     */
    @JsonField(TIMESTAMP)
    public ZonedDateTime timestamp;

    /**
     * The principalId that caused this log.
     */
    @JsonField(PRINCIPAL_ID)
    public String principalId;

    /**
     * The time the audit event took place.
     */
    @JsonField(HANDLING_TIME)
    public ZonedDateTime handlingTime;


}
