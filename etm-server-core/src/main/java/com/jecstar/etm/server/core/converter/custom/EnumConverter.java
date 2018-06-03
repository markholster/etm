package com.jecstar.etm.server.core.converter.custom;

import com.jecstar.etm.server.core.converter.CustomFieldConverter;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EnumConverter implements CustomFieldConverter<Enum> {

    private final LogWrapper log = LogFactory.getLogger(EnumConverter.class);

    private final JsonConverter jsonConverter = new JsonConverter();

    @Override
    public boolean addToJsonBuffer(String jsonKey, Enum value, StringBuilder buffer, boolean firstElement) {
        if (value != null) {
            return this.jsonConverter.addStringElementToJsonBuffer(jsonKey, value.name(), buffer, firstElement) || !firstElement;
        }
        return !firstElement;
    }

    @Override
    public void setValueOnEntity(Field field, Object entity, Object jsonValue) {
        if (jsonValue == null) {
            return;
        }
        Class<?> type = field.getType();
        try {
            Method safeValueOf = type.getDeclaredMethod("safeValueOf", String.class);
            Object enumValue = safeValueOf.invoke(null, jsonValue);
            field.set(entity, enumValue);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            log.logErrorMessage("Unable to set value '" + jsonValue + "'.", e);
        }
    }
}
