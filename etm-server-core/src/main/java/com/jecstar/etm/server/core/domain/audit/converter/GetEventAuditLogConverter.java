package com.jecstar.etm.server.core.domain.audit.converter;

import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.domain.audit.GetEventAuditLog;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;

public class GetEventAuditLogConverter extends JsonEntityConverter<GetEventAuditLog> {

    public GetEventAuditLogConverter() {
        super(GetEventAuditLog::new);
    }

    @Override
    protected boolean beforeJsonFields(GetEventAuditLog entity, StringBuilder buffer, boolean firstField) {
        boolean added = getJsonConverter().addStringElementToJsonBuffer(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.AUDIT_LOG_OBJECT_TYPE_GET_EVENT, buffer, firstField);
        return super.beforeJsonFields(entity, buffer, !added);
    }
}
