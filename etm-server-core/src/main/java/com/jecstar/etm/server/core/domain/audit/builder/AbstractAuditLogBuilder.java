package com.jecstar.etm.server.core.domain.audit.builder;

import com.jecstar.etm.server.core.domain.audit.AuditLog;

import java.time.ZonedDateTime;

abstract class AbstractAuditLogBuilder<Audit extends AuditLog<Audit>, Builder extends AbstractAuditLogBuilder<Audit, Builder>> {

    final Audit audit;

    AbstractAuditLogBuilder(Audit audit) {
        this.audit = audit;
    }

    public Audit build() {
        return this.audit;
    }

    @SuppressWarnings("unchecked")
    public Builder setTimestamp(ZonedDateTime timestamp) {
        this.audit.timestamp = timestamp;
        return (Builder) this;
    }

    @SuppressWarnings("unchecked")
    public Builder setHandlingTime(ZonedDateTime handlingTime) {
        this.audit.handlingTime = handlingTime;
        return (Builder) this;
    }

    @SuppressWarnings("unchecked")
    public Builder setPrincipalId(String principalId) {
        this.audit.principalId = principalId;
        return (Builder) this;
    }

}
