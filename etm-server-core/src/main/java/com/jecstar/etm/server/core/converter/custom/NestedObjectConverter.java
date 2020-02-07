package com.jecstar.etm.server.core.converter.custom;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.converter.CustomFieldConverter;
import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Function;

public abstract class NestedObjectConverter<T> extends JsonEntityConverter<T> implements CustomFieldConverter<T> {

    private final LogWrapper log = LogFactory.getLogger(getClass());

    private final JsonConverter jsonConverter = new JsonConverter();

    public NestedObjectConverter(Function<Map<String, Object>, T> objectFactory) {
        super(objectFactory);
    }

    @Override
    public void addToJsonBuffer(String jsonKey, T value, JsonBuilder builder) {
        if (value == null) {
            return;
        }
        builder.rawField(jsonKey, write(value));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setValueOnEntity(Field field, Object entity, Object jsonValue) {
        T value = read((Map<String, Object>) jsonValue);
        try {
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            log.logErrorMessage("Unable to set value '" + jsonValue + "'.", e);
        }
    }
}
