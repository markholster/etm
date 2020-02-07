package com.jecstar.etm.server.core.converter;

import com.jecstar.etm.domain.writer.json.JsonBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonField {

    String value();

    boolean writeWhenNull() default false;

    Class<? extends CustomFieldConverter> converterClass() default DefaultCustomConverter.class;

    final class DefaultCustomConverter implements CustomFieldConverter {
        @Override
        public void addToJsonBuffer(String jsonKey, Object entity, JsonBuilder builder) {
        }

        @Override
        public void setValueOnEntity(Field field, Object entity, Object value) {
        }
    }
}
