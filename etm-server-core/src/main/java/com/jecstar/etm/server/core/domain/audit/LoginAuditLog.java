package com.jecstar.etm.server.core.domain.audit;

/**
 * The audit log that occurs when a user executes a login.
 *
 * @author Mark Holster
 */
public class LoginAuditLog extends AuditLog<LoginAuditLog> {

    /**
     * The boolean that hold the status information of the login: Did the login attempt succeeded or failed?
     */
    public boolean success;
}
