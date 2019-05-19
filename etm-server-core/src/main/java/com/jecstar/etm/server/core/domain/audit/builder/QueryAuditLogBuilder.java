package com.jecstar.etm.server.core.domain.audit.builder;

import com.jecstar.etm.server.core.domain.audit.QueryAuditLog;

public class QueryAuditLogBuilder extends AbstractAuditLogBuilder<QueryAuditLog, QueryAuditLogBuilder> {

    public QueryAuditLogBuilder() {
        super(new QueryAuditLog());
    }

    public QueryAuditLogBuilder setUserQuery(String userQuery) {
        this.audit.userQuery = userQuery;
        return this;
    }

    public QueryAuditLogBuilder setExectuedQuery(String executedQuery) {
        this.audit.executedQuery = executedQuery;
        return this;
    }

    public QueryAuditLogBuilder setNumberOfResults(long numberOfResults) {
        this.audit.numberOfResults = numberOfResults;
        return this;
    }

    public QueryAuditLogBuilder setNumberOfResultsRelation(String numberOfResultsRelation) {
        this.audit.numberOfResultsRelation = numberOfResultsRelation;
        return this;
    }

    public QueryAuditLogBuilder setQueryTime(long queryTime) {
        this.audit.queryTime = queryTime;
        return this;
    }
}
