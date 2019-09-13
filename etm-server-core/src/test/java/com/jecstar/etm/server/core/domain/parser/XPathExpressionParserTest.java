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
        System.out.println(xml);
        XPathExpressionParser parser = new XPathExpressionParser("test", "local-name(/*)");
        String result = parser.evaluate(xml);
        assertEquals("note", result);
    }

}
