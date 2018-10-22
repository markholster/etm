package com.jecstar.etm.server.core.converter;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

/**
 * Helper class that converts an object to a json string and back.
 * <p>
 * Do not use this class on performance critical code. It uses reflection which is 10 times slower than plain java.
 *
 * @param <T> The class that contains field annotated with @{@link JsonField}
 */
public class JsonEntityConverter<T> implements EntityConverter<T, String> {

    private final LogWrapper log = LogFactory.getLogger(getClass());

    private final JsonConverter jsonConverter = new JsonConverter();

    private final Supplier<T> supplier;
    private final String jsonNamespace;
    private final List<Field> fields = new ArrayList<>();
    private final Map<Field, CustomFieldConverter> fieldConverters = new HashMap<>();

    public JsonEntityConverter(Supplier<T> supplier) {
        this.supplier = supplier;
        Class<?> clazz = this.supplier.get().getClass();
        if (clazz.isAnnotationPresent(JsonNamespace.class)) {
            JsonNamespace namespaceAnnotation = clazz.getAnnotation(JsonNamespace.class);
            this.jsonNamespace = namespaceAnnotation.value();
        } else {
            this.jsonNamespace = null;
        }
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(JsonField.class)) {
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    JsonField jsonField = field.getAnnotation(JsonField.class);
                    String jsonKey = jsonField.value();
                    if (this.fields.stream().map(f -> f.getAnnotation(JsonField.class).value()).noneMatch(jsonKey::equals)) {
                        this.fields.add(field);
                        try {
                            Constructor<? extends CustomFieldConverter> constructor = jsonField.converterClass().getDeclaredConstructor();
                            if (!constructor.isAccessible()) {
                                constructor.setAccessible(true);
                            }
                            this.fieldConverters.put(field, constructor.newInstance());
                        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e) {
                            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        fields.sort(Comparator.comparing(f -> f.getAnnotation(JsonField.class).value()));
    }

    @Override
    public T read(String value) {
        if (value == null) {
            return null;
        }
        return read(this.jsonConverter.toMap(value));
    }

    public T read(Map<String, Object> valueMap) {
        if (valueMap == null) {
            return null;
        }
        if (this.jsonNamespace != null) {
            return createEntity(this.jsonConverter.getObject(this.jsonNamespace, valueMap), this.fields);
        }
        return createEntity(valueMap, this.fields);
    }

    @Override
    public String write(T entity) {
        if (entity == null) {
            return null;
        }
        return createJson(entity, this.fields);
    }

    private T createEntity(Map<String, Object> valueMap, List<Field> fields) {
        T entity = this.supplier.get();
        for (Field field : fields) {
            JsonField jsonField = field.getAnnotation(JsonField.class);
            Class<? extends CustomFieldConverter> converterClass = jsonField.converterClass();
            Class<?> fieldType = field.getType();
            String jsonKey = jsonField.value();
            try {
                if (!valueMap.containsKey(jsonKey)) {
                    continue;
                }
                if (!converterClass.equals(JsonField.DefaultCustomConverter.class)) {
                    CustomFieldConverter fieldConverter = this.fieldConverters.get(field);
                    fieldConverter.setValueOnEntity(field, entity, valueMap.get(jsonKey));
                    continue;
                }
                if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
                    Long value = this.jsonConverter.getLong(jsonKey, valueMap);
                    if (fieldType.equals(Long.class)) {
                        field.set(entity, value);
                    } else if (value == null) {
                        field.setLong(entity, 0L);
                    } else {
                        field.setLong(entity, value);
                    }
                } else if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
                    Double value = this.jsonConverter.getDouble(jsonKey, valueMap);
                    if (fieldType.equals(Double.class)) {
                        field.set(entity, value);
                    } else if (value == null) {
                        field.setDouble(entity, 0D);
                    } else {
                        field.setDouble(entity, value);
                    }
                } else if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
                    Integer value = this.jsonConverter.getInteger(jsonKey, valueMap);
                    if (fieldType.equals(Integer.class)) {
                        field.set(entity, value);
                    } else if (value == null) {
                        field.setInt(entity, 0);
                    } else {
                        field.setInt(entity, value);
                    }
                } else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
                    Boolean value = this.jsonConverter.getBoolean(jsonKey, valueMap);
                    if (fieldType.equals(Boolean.class)) {
                        field.set(entity, value);
                    } else if (value == null) {
                        field.setBoolean(entity, false);
                    } else {
                        field.setBoolean(entity, value);
                    }
                } else if (fieldType.equals(Instant.class)) {
                    Long value = this.jsonConverter.getLong(jsonKey, valueMap);
                    if (value == null) {
                        field.set(entity, null);
                    } else {
                        Instant instant = Instant.ofEpochMilli(value);
                        field.set(entity, instant);
                    }
                } else {
                    field.set(entity, valueMap.get(jsonKey));
                }
            } catch (IllegalAccessException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage(e.getMessage(), e);
                }
            }
        }
        return entity;
    }


    @SuppressWarnings("unchecked")
    private String createJson(T entity, List<Field> fields) {
        StringBuilder result = new StringBuilder();
        result.append("{");
        if (this.jsonNamespace != null) {
            this.jsonConverter.addStringElementToJsonBuffer(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, this.jsonNamespace, result, true);
            result.append(", " + this.jsonConverter.escapeToJson(this.jsonNamespace, true) + ": {");
        }
        boolean added = beforeJsonFields(entity, result, true);
        for (Field field : fields) {
            JsonField jsonField = field.getAnnotation(JsonField.class);
            Class<? extends CustomFieldConverter> converterClass = jsonField.converterClass();
            Class<?> fieldType = field.getType();
            String jsonKey = jsonField.value();
            try {
                Object value = field.get(entity);
                if (!converterClass.equals(JsonField.DefaultCustomConverter.class)) {
                    CustomFieldConverter fieldConverter = this.fieldConverters.get(field);
                    added = fieldConverter.addToJsonBuffer(jsonKey, value, result, !added) || added;
                    continue;
                }
                added = addElementToJson(fieldType, value, jsonKey, jsonField.writeWhenNull(), result, !added) || added;
            } catch (IllegalAccessException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage(e.getMessage(), e);
                }
            }
        }
        afterJsonFields(entity, result, added);
        if (this.jsonNamespace != null) {
            result.append("}");
        }
        result.append("}");
        return result.toString();
    }

    private boolean addElementToJson(Class entityClass, Object value, String jsonKey, boolean writeWhenNull, StringBuilder buffer, boolean firstElement) {
        boolean added = !firstElement;
        if (entityClass.equals(String.class)) {
            added = this.jsonConverter.addStringElementToJsonBuffer(jsonKey, (String) value, writeWhenNull, buffer, !added) || added;
        } else if (entityClass.equals(Long.class) || entityClass.equals(long.class)) {
            added = this.jsonConverter.addLongElementToJsonBuffer(jsonKey, (Long) value, writeWhenNull, buffer, !added) || added;
        } else if (entityClass.equals(Double.class) || entityClass.equals(double.class)) {
            added = this.jsonConverter.addDoubleElementToJsonBuffer(jsonKey, (Double) value, writeWhenNull, buffer, !added) || added;
        } else if (entityClass.equals(Integer.class) || entityClass.equals(int.class)) {
            added = this.jsonConverter.addIntegerElementToJsonBuffer(jsonKey, (Integer) value, writeWhenNull, buffer, !added) || added;
        } else if (entityClass.equals(Boolean.class) || entityClass.equals(boolean.class)) {
            added = this.jsonConverter.addBooleanElementToJsonBuffer(jsonKey, (Boolean) value, writeWhenNull, buffer, !added) || added;
        } else if (entityClass.equals(Instant.class)) {
            Instant instant = (Instant) value;
            added = this.jsonConverter.addInstantElementToJsonBuffer(jsonKey, instant, writeWhenNull, buffer, !added) || added;
        } else if (entityClass.equals(List.class)) {
            List<?> list = (List<?>) value;
            if (list == null) {
                if (writeWhenNull) {
                    if (!firstElement) {
                        buffer.append(",");
                    }
                    buffer.append(this.jsonConverter.escapeToJson(jsonKey, true));
                    buffer.append(": []");
                    added = true;
                }
            } else {
                if (!firstElement) {
                    buffer.append(",");
                }
                buffer.append(this.jsonConverter.escapeToJson(jsonKey, true));
                buffer.append(": [");
                for (int i = 0; i < list.size(); i++) {
                    addElementToJson(list.get(i).getClass(), list.get(i), null, false, buffer, i == 0);
                }
                buffer.append("]");
                added = true;
            }
        }
        return added;
    }

    /**
     * Callback method that will be called before the entity is converted to a json string.
     *
     * @param entity     The entity that will be converted to json.
     * @param buffer     The json buffer.
     * @param firstField Boolean indicating this is the first element to be written.
     * @return <code>true</code> when this method has added a field, <code>false</code> otherwise.
     */
    protected boolean beforeJsonFields(T entity, StringBuilder buffer, boolean firstField) {
        return !firstField;
    }

    /**
     * Callback method that will be called after the entity is converted to a json string.
     *
     * @param entity     The entity that is converted to json.
     * @param buffer     The json buffer.
     * @param firstField Boolean indicating this is the first element to be written.
     * @return <code>true</code> when this method has added a field, <code>false</code> otherwise.
     */
    protected boolean afterJsonFields(T entity, StringBuilder buffer, boolean firstField) {
        return !firstField;
    }

    protected JsonConverter getJsonConverter() {
        return this.jsonConverter;
    }

}
