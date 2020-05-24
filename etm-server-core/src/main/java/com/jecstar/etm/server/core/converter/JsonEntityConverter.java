/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.server.core.converter;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
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
import java.util.function.Function;

/**
 * Helper class that converts an object to a json string and back.
 * <p>
 * Do not use this class on performance critical code. It uses reflection which is 10 times slower than plain java.
 *
 * @param <T> The class that contains field annotated with @{@link JsonField}
 */
public class JsonEntityConverter<T> implements EntityConverter<T, String> {

    protected final LogWrapper log = LogFactory.getLogger(getClass());

    protected final JsonConverter jsonConverter = new JsonConverter();

    private final Function<Map<String, Object>, T> objectFactory;

    private final Map<Class<?>, ClassMetadata> classMetadataCache = new HashMap<>();

    public JsonEntityConverter(Function<Map<String, Object>, T> objectFactory) {
        this.objectFactory = objectFactory;
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
        return createEntity(valueMap);
    }

    @Override
    public String write(T entity) {
        if (entity == null) {
            return null;
        }
        return createJson(entity);
    }

    private T createEntity(Map<String, Object> valueMap) {
        T entity = this.objectFactory.apply(valueMap);
        final Class<?> clazz = entity.getClass();
        ClassMetadata classMetadata = this.classMetadataCache.get(clazz);
        if (classMetadata == null) {
            classMetadata = createClassMetadata(clazz);
            this.classMetadataCache.put(clazz, classMetadata);
        }
        if (classMetadata.getJsonNamespace() != null) {
            valueMap = this.jsonConverter.getObject(classMetadata.jsonNamespace, valueMap);
        }
        for (Field field : classMetadata.getFields()) {
            JsonField jsonField = field.getAnnotation(JsonField.class);
            Class<? extends CustomFieldConverter> converterClass = jsonField.converterClass();
            Class<?> fieldType = field.getType();
            String jsonKey = jsonField.value();
            try {
                if (!valueMap.containsKey(jsonKey)) {
                    continue;
                }
                if (!converterClass.equals(JsonField.DefaultCustomConverter.class)) {
                    CustomFieldConverter fieldConverter = classMetadata.getFieldConverters().get(field);
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
                } else if (fieldType.equals(Float.class) || fieldType.equals(float.class)) {
                    Float value = this.jsonConverter.getFloat(jsonKey, valueMap);
                    if (fieldType.equals(Float.class)) {
                        field.set(entity, value);
                    } else if (value == null) {
                        field.setFloat(entity, 0F);
                    } else {
                        field.setFloat(entity, value);
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

    private ClassMetadata createClassMetadata(Class<?> clazz) {
        String jsonNamespace = null;
        if (clazz.isAnnotationPresent(JsonNamespace.class)) {
            JsonNamespace namespaceAnnotation = clazz.getAnnotation(JsonNamespace.class);
            jsonNamespace = namespaceAnnotation.value();
        }
        ClassMetadata classMetadata = new ClassMetadata(jsonNamespace);
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(JsonField.class)) {
                    field.setAccessible(true);
                    JsonField jsonField = field.getAnnotation(JsonField.class);
                    String jsonKey = jsonField.value();
                    if (classMetadata.getFields().stream().map(f -> f.getAnnotation(JsonField.class).value()).noneMatch(jsonKey::equals)) {
                        classMetadata.getFields().add(field);
                        try {
                            Constructor<? extends CustomFieldConverter> constructor = jsonField.converterClass().getDeclaredConstructor();
                            constructor.setAccessible(true);
                            classMetadata.getFieldConverters().put(field, constructor.newInstance());
                        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e) {
                            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        classMetadata.getFields().sort(Comparator.comparing(f -> f.getAnnotation(JsonField.class).value()));
        return classMetadata;
    }


    @SuppressWarnings("unchecked")
    private String createJson(T entity) {
        JsonBuilder builder = new JsonBuilder();
        builder.startObject();
        Class<?> clazz = entity.getClass();
        ClassMetadata classMetadata = this.classMetadataCache.get(entity.getClass());
        if (classMetadata == null) {
            classMetadata = createClassMetadata(clazz);
            this.classMetadataCache.put(clazz, classMetadata);
        }
        if (classMetadata.getJsonNamespace() != null) {
            builder.field(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, classMetadata.getJsonNamespace());
            builder.startObject(classMetadata.getJsonNamespace());
        }
        beforeJsonFields(entity, builder);
        for (Field field : classMetadata.getFields()) {
            JsonField jsonField = field.getAnnotation(JsonField.class);
            Class<? extends CustomFieldConverter> converterClass = jsonField.converterClass();
            Class<?> fieldType = field.getType();
            String jsonKey = jsonField.value();
            try {
                Object value = field.get(entity);
                if (!converterClass.equals(JsonField.DefaultCustomConverter.class)) {
                    CustomFieldConverter fieldConverter = classMetadata.getFieldConverters().get(field);
                    fieldConverter.addToJsonBuffer(jsonKey, value, builder);
                    continue;
                }
                addElementToJson(fieldType, value, jsonKey, jsonField.writeWhenNull(), false, builder);
            } catch (IllegalAccessException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage(e.getMessage(), e);
                }
            }
        }
        afterJsonFields(entity, builder);
        if (classMetadata.getJsonNamespace() != null) {
            builder.endObject();
        }
        builder.endObject();
        return builder.build();
    }

    private void addElementToJson(Class entityClass, Object value, String jsonKey, boolean writeWhenNull, boolean asArrayValue, JsonBuilder builder) {
        Class<?> classToTest = entityClass;
        if (value != null) {
            classToTest = value.getClass();
        }

        if (String.class.isAssignableFrom(classToTest)) {
            if (asArrayValue) {
                builder.rawElement(JsonBuilder.escapeToJson((String) value, true));
            } else {
                builder.field(jsonKey, (String) value, writeWhenNull);
            }
        } else if (Long.class.isAssignableFrom(classToTest) || long.class.isAssignableFrom(classToTest)) {
            if (asArrayValue) {
                builder.rawElement(value.toString());
            } else {
                builder.field(jsonKey, (Long) value, writeWhenNull);
            }
        } else if (Double.class.isAssignableFrom(classToTest) || double.class.isAssignableFrom(classToTest)) {
            if (asArrayValue) {
                builder.rawElement(value.toString());
            } else {
                builder.field(jsonKey, (Double) value, writeWhenNull);
            }
        } else if (Float.class.isAssignableFrom(classToTest) || float.class.isAssignableFrom(classToTest)) {
            if (asArrayValue) {
                builder.rawElement(value.toString());
            } else {
                builder.field(jsonKey, (Float) value, writeWhenNull);
            }
        } else if (Integer.class.isAssignableFrom(classToTest) || int.class.isAssignableFrom(classToTest)) {
            if (asArrayValue) {
                builder.rawElement(value.toString());
            } else {
                builder.field(jsonKey, (Integer) value, writeWhenNull);
            }
        } else if (Boolean.class.isAssignableFrom(classToTest) || boolean.class.isAssignableFrom(classToTest)) {
            if (asArrayValue) {
                builder.rawElement(value.toString());
            } else {
                builder.field(jsonKey, (Boolean) value, writeWhenNull);
            }
        } else if (Instant.class.isAssignableFrom(classToTest)) {
            if (asArrayValue) {
                builder.rawElement("" + ((Instant) value).toEpochMilli());
            } else {
                builder.field(jsonKey, (Instant) value, writeWhenNull);
            }
        } else if (Collection.class.isAssignableFrom(classToTest)) {
            Collection<?> collection = (Collection<?>) value;
            if (collection == null) {
                if (writeWhenNull) {
                    builder.startArray(jsonKey).endArray();
                }
            } else {
                builder.startArray(jsonKey);
                for (var item : collection) {
                    if (item != null) {
                        addElementToJson(item.getClass(), item, null, false, true, builder);
                    }
                }
                builder.endArray();
            }
        }
    }

    /**
     * Callback method that will be called before the entity is converted to a json string.
     *
     * @param entity  The entity that will be converted to json.
     * @param builder The json builder.
     */
    protected void beforeJsonFields(T entity, JsonBuilder builder) {
    }

    /**
     * Callback method that will be called after the entity is converted to a json string.
     *
     * @param entity  The entity that is converted to json.
     * @param builder The json builder.
     */
    protected void afterJsonFields(T entity, JsonBuilder builder) {
    }

    protected JsonConverter getJsonConverter() {
        return this.jsonConverter;
    }


    private class ClassMetadata {

        final String jsonNamespace;
        final List<Field> fields = new ArrayList<>();
        final Map<Field, CustomFieldConverter> fieldConverters = new HashMap<>();

        ClassMetadata(String jsonNamespace) {
            this.jsonNamespace = jsonNamespace;
        }

        public String getJsonNamespace() {
            return this.jsonNamespace;
        }

        public List<Field> getFields() {
            return this.fields;
        }

        public Map<Field, CustomFieldConverter> getFieldConverters() {
            return this.fieldConverters;
        }
    }
}
