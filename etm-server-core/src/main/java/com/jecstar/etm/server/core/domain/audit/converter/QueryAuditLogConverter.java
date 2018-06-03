package com.jecstar.etm.server.core.domain.audit.converter;

import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.domain.audit.QueryAuditLog;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;

public class QueryAuditLogConverter extends JsonEntityConverter<QueryAuditLog> {

    public QueryAuditLogConverter() {
        super(QueryAuditLog::new);
    }

    @Override
    protected boolean beforeJsonFields(QueryAuditLog entity, StringBuilder buffer, boolean firstField) {
        boolean added = getJsonConverter().addStringElementToJsonBuffer(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.AUDIT_LOG_OBJECT_TYPE_SEARCH, buffer, firstField);
        return super.beforeJsonFields(entity, buffer, !added);
    }
}
