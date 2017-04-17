package com.jecstar.etm.server.core.domain.audit;

import java.time.ZonedDateTime;

public abstract class AuditLog<T extends AuditLog<T>> {

	
	/**
	 * The principalId that caused this log.
	 */
	public String principalId;
	
	/**
	 * The time the audit event took place.
	 */
	public ZonedDateTime handlingTime;

	
}
