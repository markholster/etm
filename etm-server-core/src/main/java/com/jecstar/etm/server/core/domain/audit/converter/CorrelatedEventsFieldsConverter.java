package com.jecstar.etm.server.core.domain.audit.converter;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.converter.CustomFieldConverter;
import com.jecstar.etm.server.core.domain.audit.GetEventAuditLog;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CorrelatedEventsFieldsConverter implements CustomFieldConverter<Map<String, Object>> {

    private final JsonConverter jsonConverter = new JsonConverter();

    @Override
    public boolean addToJsonBuffer(String jsonKey, Map<String, Object> value, StringBuilder buffer, boolean firstElement) {
        if (value != null && value.size() > 0) {
            if (!firstElement) {
                buffer.append(",");
            }
            buffer.append(this.jsonConverter.escapeToJson(jsonKey, true)).append(": [");
            buffer.append(value.entrySet().stream()
                    .map(c -> "{" + this.jsonConverter.escapeObjectToJsonNameValuePair(GetEventAuditLog.EVENT_ID, c.getKey()) + "," + this.jsonConverter.escapeObjectToJsonNameValuePair(GetEventAuditLog.EVENT_TYPE, c.getValue()) + "}")
                    .collect(Collectors.joining(","))
            );
            buffer.append("]");
            return true;
        }
        return !firstElement;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValueOnEntity(Field field, Object entity, Object jsonValue) {
        if (jsonValue == null) {
            return;
        }
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> valueList = (List<Map<String, Object>>) jsonValue;
        for (Map<String, Object> value : valueList) {
            String key = value.keySet().iterator().next();
            result.put(key, value.get(key));
        }
        if (result.size() > 0) {
            try {
                Map<String, Object> currentValue = (Map<String, Object>) field.get(entity);
                currentValue.putAll(result);
            } catch (IllegalAccessException e) {
                throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
            }
        }
    }
}
