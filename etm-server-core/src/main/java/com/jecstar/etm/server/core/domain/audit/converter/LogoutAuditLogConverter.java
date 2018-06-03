package com.jecstar.etm.server.core.domain.audit.converter;

import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.domain.audit.LogoutAuditLog;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;

public class LogoutAuditLogConverter extends JsonEntityConverter<LogoutAuditLog> {

    public LogoutAuditLogConverter() {
        super(LogoutAuditLog::new);
    }

    @Override
    protected boolean beforeJsonFields(LogoutAuditLog entity, StringBuilder buffer, boolean firstField) {
        boolean added = getJsonConverter().addStringElementToJsonBuffer(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.AUDIT_LOG_OBJECT_TYPE_LOGOUT, buffer, firstField);
        return super.beforeJsonFields(entity, buffer, !added);
    }
}
