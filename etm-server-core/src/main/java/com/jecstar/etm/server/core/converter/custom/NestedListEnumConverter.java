package com.jecstar.etm.server.core.converter.custom;

import com.jecstar.etm.server.core.converter.CustomFieldConverter;
import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NestedListEnumConverter<K extends Enum, T extends List<K>> extends JsonEntityConverter<T> implements CustomFieldConverter<T> {

    private final LogWrapper log = LogFactory.getLogger(getClass());
    private final JsonConverter jsonConverter = new JsonConverter();
    private final Function<String, K> entityConverter;

    @SuppressWarnings("unchecked")
    public NestedListEnumConverter(Function<String, K> entityConverter) {
        super(f -> (T) new ArrayList());
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
        result.append(this.jsonConverter.escapeToJson(jsonKey, true));
        result.append(": [");
        result.append(values.stream().map(Enum::name).collect(Collectors.joining(",")));
        result.append("]");
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setValueOnEntity(Field field, Object entity, Object jsonValue) {
        List<String> jsonObjects = (List<String>) jsonValue;
        List<K> values = jsonObjects.stream().map(this.entityConverter).filter(Objects::nonNull).collect(Collectors.toList());
        try {
            field.set(entity, values);
        } catch (IllegalAccessException e) {
            log.logErrorMessage("Unable to set value '" + jsonValue + "'.", e);
        }
    }
}
