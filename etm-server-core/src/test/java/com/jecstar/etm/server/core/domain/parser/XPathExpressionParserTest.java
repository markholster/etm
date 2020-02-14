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

package com.jecstar.etm.server.core.domain.parser;

import net.sf.saxon.om.NamePool;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class XPathExpressionParserTest {

    @Test
    public void testNamePoolLimitExceptionWorkaround() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field declaredField = NamePool.class.getDeclaredField("MAX_FINGERPRINT");
        declaredField.setAccessible(true);
        Integer maxPrefixesPerUri = (Integer) declaredField.get(NamePool.class);
        XPathExpressionParser parser = new XPathExpressionParser("test", "local-name(/*)");
        for (int i = 0; i <= maxPrefixesPerUri; i++) {
            String result = parser.evaluate("<ns" + i + ":test xmlns:ns" + i + "=\"http://www.test.com\">NamePoolLimitTest</ns" + i + ":test>");
            assertEquals("test", result);
        }
    }

    @Test
    public void testInvalidDtdLocation() {
        final String xml = "<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE note SYSTEM \"http://invalid.com/note.dtd\">\n" +
                "<note>\n" +
                "  <to>Tove</to>\n" +
                "  <from>Jani</from>\n" +
                "  <heading>Reminder</heading>\n" +
                "  <body>Don't forget me this weekend!</body>\n" +
                "</note>";
        XPathExpressionParser parser = new XPathExpressionParser("test", "local-name(/*)");
        String result = parser.evaluate(xml);
        assertEquals("note", result);
    }

    @Test
    public void testInvalidSchemaLocation() {
        final String xml = "<?xml version=\"1.0\"?>\n" +
                "<note xmlns=\"https://www.jecstar.com\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://www.jecstar.com http://invalid.com/note.xsd\">\n" +
                "  <to>Tove</to>\n" +
                "  <from>Jani</from>\n" +
                "  <heading>Reminder</heading>\n" +
                "  <body>Don't forget me this weekend!</body>\n" +
                "</note>";
        XPathExpressionParser parser = new XPathExpressionParser("test", "local-name(/*)");
        String result = parser.evaluate(xml);
        assertEquals("note", result);
    }

}
