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

package com.jecstar.etm.server.core.converter.custom;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NestedListEnumConverterTest {

    private enum MyEnum {
        ONE, TWO, THREE;

        public static MyEnum safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return MyEnum.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private class EnumListHolder {

        private List<MyEnum> enums;
    }

    @Test
    void testEnumListConversion() throws NoSuchFieldException {
        var converter = new NestedListEnumConverter<>(s -> {
            try {
                Method safeValueOf = MyEnum.class.getDeclaredMethod("safeValueOf", String.class);
                return (MyEnum) safeValueOf.invoke(null, s);
            } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                return null;
            }
        });

        var enums = new ArrayList<MyEnum>();
        enums.add(MyEnum.ONE);
        enums.add(MyEnum.TWO);
        enums.add(MyEnum.THREE);
        var builder = new JsonBuilder();
        builder.startObject();
        converter.addToJsonBuffer("enums", enums, builder);
        builder.endObject();

        assertEquals("{\"enums\":[\"ONE\",\"TWO\",\"THREE\"]}", builder.build());

        var enumHolder = new EnumListHolder();
        Field field = enumHolder.getClass().getDeclaredField("enums");
        field.setAccessible(true);
        converter.setValueOnEntity(field, enumHolder, List.of("ONE", "TWO", "THREE"));
        assertEquals(enums.size(), enumHolder.enums.size());
    }

}


