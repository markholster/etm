package com.jecstar.etm.server.core.converter.custom;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.converter.CustomFieldConverter;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Collectors;

public class MapKeyValueConverter implements CustomFieldConverter<Map<String, Object>> {

    private final JsonConverter jsonConverter = new JsonConverter();

    @Override
    public boolean addToJsonBuffer(String jsonKey, Map<String, Object> value, StringBuilder buffer, boolean firstElement) {
        if (value != null && value.size() > 0) {
            if (!firstElement) {
                buffer.append(",");
            }
            buffer.append(this.jsonConverter.escapeToJson(jsonKey, true)).append(": {");
            buffer.append(value.entrySet().stream()
                    .map(c -> "{" + this.jsonConverter.escapeObjectToJsonNameValuePair(c.getKey(), c.getValue()) + "}")
                    .collect(Collectors.joining(","))
            );
            buffer.append("}");
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValueOnEntity(Field field, Object entity, Object jsonValue) {
        if (jsonValue == null) {
            return;
        }
        Map<String, Object> valueMap = (Map<String, Object>) jsonValue;
        try {
            field.set(valueMap, entity);
        } catch (IllegalAccessException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }
}
