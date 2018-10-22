package com.jecstar.etm.server.core.domain.audit.builder;

import com.jecstar.etm.server.core.domain.audit.AuditLog;

import java.time.Instant;

abstract class AbstractAuditLogBuilder<Audit extends AuditLog, Builder extends AbstractAuditLogBuilder<Audit, Builder>> {

    final Audit audit;

    AbstractAuditLogBuilder(Audit audit) {
        this.audit = audit;
    }

    public Audit build() {
        return this.audit;
    }

    @SuppressWarnings("unchecked")
    public Builder setTimestamp(Instant timestamp) {
        this.audit.timestamp = timestamp;
        return (Builder) this;
    }

    public Instant getTimestamp() {
        return this.audit.timestamp;
    }

    @SuppressWarnings("unchecked")
    public Builder setHandlingTime(Instant handlingTime) {
        this.audit.handlingTime = handlingTime;
        return (Builder) this;
    }

    @SuppressWarnings("unchecked")
    public Builder setPrincipalId(String principalId) {
        this.audit.principalId = principalId;
        return (Builder) this;
    }

}
