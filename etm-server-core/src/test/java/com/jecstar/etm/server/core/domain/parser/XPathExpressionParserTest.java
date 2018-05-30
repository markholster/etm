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
}
