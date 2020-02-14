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

package com.jecstar.etm.domain.writer.json;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for the <code>JsonBuilder</code> class.
 */
public class JsonBuilderTest {

    @Test
    public void testEmptyRootObject() {
        var builder = new JsonBuilder();
        builder.startObject().endObject();
        assertEquals("{}", builder.build());
    }

    @Test
    public void testEmptyObject() {
        var builder = new JsonBuilder();
        builder.startObject().startObject("empty").endObject().endObject();
        assertEquals("{\"empty\":{}}", builder.build());
    }

    @Test
    public void testEmptyArray() {
        var builder = new JsonBuilder();
        builder.startObject().startArray("empty").endArray().endObject();
        assertEquals("{\"empty\":[]}", builder.build());
    }

    @Test
    public void testFieldAsString() {
        var date = new Date();
        var number = 3L;
        var bool = true;
        var text = "Hello!";
        var builder = new JsonBuilder();
        builder.startObject();
        builder.fieldAsString("null", null)
                .fieldAsString("number", number)
                .fieldAsString("boolean", true)
                .fieldAsString("date", date)
                .fieldAsString("string", text);
        builder.endObject();
        String expected = "{\"null\":\"null\",\"number\":\"" + number + "\",\"number_as_number\":" + number
                + ",\"boolean\":\"" + bool + "\",\"boolean_as_boolean\":" + bool
                + ",\"date\":\"" + date.toString() + "\",\"date_as_date\":" + date.getTime()
                + ",\"string\":\"" + text + "\"}";
        assertEquals(expected, builder.build());
    }

    @Test
    public void testStringArray() {
        var builder = new JsonBuilder();
        builder.startObject().field("array", "one", "two").endObject();
        assertEquals("{\"array\":[\"one\",\"two\"]}", builder.build());
    }

    @Test
    public void testObjectArray() {
        var builder = new JsonBuilder();
        builder.startObject()
                .startArray("array")
                .startObject()
                .field("f1", "v1")
                .endObject()
                .startObject()
                .field("f2", "v2")
                .endObject()
                .endArray()
                .endObject();
        assertEquals("{\"array\":[{\"f1\":\"v1\"},{\"f2\":\"v2\"}]}", builder.build());
    }

    @Test
    public void testRawArray() {
        var builder = new JsonBuilder();
        builder.startObject();
        builder.startArray("raw").rawElement("{\"test_1\":1}").rawElement("{\"test_2\":2}").endArray();
        builder.endObject();
        assertEquals("{\"raw\":[{\"test_1\":1},{\"test_2\":2}]}", builder.build());

        builder = new JsonBuilder();
        builder.startObject();
        builder.startArray("raw").rawElement("1").rawElement("2").endArray();
        builder.endObject();
        assertEquals("{\"raw\":[1,2]}", builder.build());
    }
}
