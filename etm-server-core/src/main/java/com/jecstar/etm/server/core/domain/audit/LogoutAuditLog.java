package com.jecstar.etm.server.core.domain.audit;

/**
 * The audit log that occurs when a user executes a logout or the session is destroyed.
 * 
 * @author Mark Holster
 */
public class LogoutAuditLog extends AuditLog<LogoutAuditLog> {

	/**
	 * The boolean that indicates if the logout action was because of a session expiration or not.
	 */
	public boolean expired;
}
