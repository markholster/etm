package com.jecstar.etm.server.core.converter;

import java.lang.reflect.Field;

public interface CustomFieldConverter<T> {

    boolean addToJsonBuffer(String jsonKey, T value, StringBuilder result, boolean firstElement);

    void setValueOnEntity(Field field, Object entity, Object jsonValue);
}
