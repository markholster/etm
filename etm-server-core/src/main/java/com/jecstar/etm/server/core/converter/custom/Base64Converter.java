package com.jecstar.etm.server.core.converter.custom;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.converter.CustomFieldConverter;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.lang.reflect.Field;

public class Base64Converter implements CustomFieldConverter<String> {

    private final LogWrapper log = LogFactory.getLogger(Base64Converter.class);

    private final JsonConverter jsonConverter = new JsonConverter();

    private final int rounds = 7;

    @Override
    public void addToJsonBuffer(String jsonKey, String value, JsonBuilder builder) {
        if (value != null) {
            builder.field(jsonKey, this.jsonConverter.encodeBase64(value, rounds));
        }
    }

    @Override
    public void setValueOnEntity(Field field, Object entity, Object jsonValue) {
        if (jsonValue == null) {
            return;
        }
        try {
            field.set(entity, this.jsonConverter.decodeBase64(jsonValue.toString(), rounds));
        } catch (IllegalAccessException e) {
            log.logErrorMessage("Unable to set value '" + jsonValue + "'.", e);
        }
    }
}
