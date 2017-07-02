package com.jecstar.etm.server.core.domain.converter;

import com.jecstar.etm.server.core.domain.audit.AuditLog;

public interface AuditLogConverter<T, Audit extends AuditLog<Audit>> {

	Audit read(T content);
	
	T write(Audit audit);
	
	AuditLogTags getTags();
}
