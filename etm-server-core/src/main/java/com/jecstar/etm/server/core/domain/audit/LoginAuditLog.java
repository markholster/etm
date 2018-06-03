package com.jecstar.etm.server.core.domain.audit;

import com.jecstar.etm.server.core.converter.JsonField;

/**
 * The audit log that occurs when a user executes a login.
 *
 * @author Mark Holster
 */
public class LoginAuditLog extends AuditLog {

    /**
     * The boolean that hold the status information of the login: Did the login attempt succeeded or failed?
     */
    @JsonField("success")
    public boolean success;
}
