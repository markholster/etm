package com.jecstar.etm.server.core.domain.audit.converter;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.converter.CustomFieldConverter;
import com.jecstar.etm.server.core.domain.audit.GetEventAuditLog;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class CorrelatedEventsFieldsConverter implements CustomFieldConverter<Set<String>> {

    private final JsonConverter jsonConverter = new JsonConverter();

    @Override
    public boolean addToJsonBuffer(String jsonKey, Set<String> value, StringBuilder buffer, boolean firstElement) {
        if (value != null && value.size() > 0) {
            if (!firstElement) {
                buffer.append(",");
            }
            buffer.append(this.jsonConverter.escapeToJson(jsonKey, true)).append(": [");
            buffer.append(value.stream()
                    .map(c -> this.jsonConverter.escapeToJson(c, true))
                    .collect(Collectors.joining(","))
            );
            buffer.append("]");
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
        Set<String> result = new HashSet<>();
        List<String> valueList = (List<String>) jsonValue;
        if (valueList != null) {
            result.addAll(valueList);
        }
        if (result.size() > 0) {
            try {
                Set<String> currentValue = (Set<String>) field.get(entity);
                currentValue.addAll(result);
            } catch (IllegalAccessException e) {
                throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
            }
        }
    }
}
