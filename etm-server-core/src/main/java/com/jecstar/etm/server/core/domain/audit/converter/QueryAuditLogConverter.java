package com.jecstar.etm.server.core.domain.audit.converter;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.domain.audit.QueryAuditLog;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;

public class QueryAuditLogConverter extends JsonEntityConverter<QueryAuditLog> {

    public QueryAuditLogConverter() {
        super(f -> new QueryAuditLog());
    }

    @Override
    protected void beforeJsonFields(QueryAuditLog entity, JsonBuilder builder) {
        builder.field(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.AUDIT_LOG_OBJECT_TYPE_SEARCH);
        super.beforeJsonFields(entity, builder);
    }
}
