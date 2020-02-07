package com.jecstar.etm.server.core.domain.audit.converter;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.domain.audit.LogoutAuditLog;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;

public class LogoutAuditLogConverter extends JsonEntityConverter<LogoutAuditLog> {

    public LogoutAuditLogConverter() {
        super(f -> new LogoutAuditLog());
    }

    @Override
    protected void beforeJsonFields(LogoutAuditLog entity, JsonBuilder builder) {
        builder.field(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.AUDIT_LOG_OBJECT_TYPE_LOGOUT);
        super.beforeJsonFields(entity, builder);
    }
}
