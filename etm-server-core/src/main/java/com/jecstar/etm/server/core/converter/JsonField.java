package com.jecstar.etm.server.core.converter;

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
        public boolean addToJsonBuffer(String jsonKey, Object entity, StringBuilder result, boolean firstValue) {
            return !firstValue;
        }

        @Override
        public void setValueOnEntity(Field field, Object entity, Object value) {
        }
    }
}
