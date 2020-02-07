package com.jecstar.etm.server.core.converter;

import com.jecstar.etm.domain.writer.json.JsonBuilder;

import java.lang.reflect.Field;

public interface CustomFieldConverter<T> {

    void addToJsonBuffer(String jsonKey, T value, JsonBuilder builder);

    void setValueOnEntity(Field field, Object entity, Object jsonValue);
}
