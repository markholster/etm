package com.jecstar.etm.server.core.domain.audit;

import com.jecstar.etm.server.core.converter.JsonField;

/**
 * The audit log that occurs when a user executes a logout or the session is destroyed.
 *
 * @author Mark Holster
 */
public class LogoutAuditLog extends AuditLog {

    /**
     * The boolean that indicates if the logout action was because of a session expiration or not.
     */
    @JsonField("expired")
    public boolean expired;
}