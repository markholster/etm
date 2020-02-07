package com.jecstar.etm.server.core.domain.audit.converter;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.domain.audit.LoginAuditLog;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;

public class LoginAuditLogConverter extends JsonEntityConverter<LoginAuditLog> {

    public LoginAuditLogConverter() {
        super(f -> new LoginAuditLog());
    }

    @Override
    protected void beforeJsonFields(LoginAuditLog entity, JsonBuilder builder) {
        builder.field(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.AUDIT_LOG_OBJECT_TYPE_LOGIN);
        super.beforeJsonFields(entity, builder);
    }
}
