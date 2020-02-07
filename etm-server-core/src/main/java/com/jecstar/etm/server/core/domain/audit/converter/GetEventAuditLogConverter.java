package com.jecstar.etm.server.core.domain.audit.converter;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.domain.audit.GetEventAuditLog;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;

public class GetEventAuditLogConverter extends JsonEntityConverter<GetEventAuditLog> {

    public GetEventAuditLogConverter() {
        super(f -> new GetEventAuditLog());
    }

    @Override
    protected void beforeJsonFields(GetEventAuditLog entity, JsonBuilder builder) {
        builder.field(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.AUDIT_LOG_OBJECT_TYPE_GET_EVENT);
        super.beforeJsonFields(entity, builder);
    }
}
