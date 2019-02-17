package com.jecstar.etm.server.core.converter.custom;

import com.jecstar.etm.server.core.converter.CustomFieldConverter;
import com.jecstar.etm.server.core.converter.JsonEntityConverter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NestedNamedMapObjectConverter<V, T extends Map<String, V>> extends JsonEntityConverter<T> implements CustomFieldConverter<T> {

    private final JsonEntityConverter<V> entityConverter;

    @SuppressWarnings("unchecked")
    public NestedNamedMapObjectConverter(JsonEntityConverter<V> entityConverter) {
        super(f -> (T) new HashMap<String, V>());
        this.entityConverter = entityConverter;
    }

    @Override
    public boolean addToJsonBuffer(String jsonKey, T values, StringBuilder result, boolean firstElement) {
        if (values == null) {
            return false;
        }
        if (!firstElement) {
            result.append(", ");
        }
        result.append(jsonConverter.escapeToJson(jsonKey, true));
        result.append(": {");
        result.append(values.entrySet().stream()
                .map(e -> jsonConverter.escapeToJson(e.getKey(), true) + ":" + this.entityConverter.write(e.getValue()))
                .collect(Collectors.joining(",")));
        result.append("}");
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValueOnEntity(Field field, Object entity, Object jsonValue) {
        Map<String, V> results = new HashMap<>();

        Map<String, Object> rootObject = (Map<String, Object>) jsonValue;
        Set<String> names = rootObject.keySet();
        for (String name : names) {
            Map<String, Object> values = jsonConverter.getObject(name, rootObject);
            results.put(name, this.entityConverter.read(values));
        }
        try {
            field.set(entity, results);
        } catch (IllegalAccessException e) {
            log.logErrorMessage("Unable to set value '" + jsonValue + "'.", e);
        }
    }
}
