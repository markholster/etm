package com.jecstar.etm.core.parsers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.sf.saxon.om.NamePool;

public class XPathExpressionParserTest {

	@Test
	public void testNamePoolLimitExceptionWorkaround() {
		XPathExpressionParser parser = new XPathExpressionParser("local-name(/*)");
		for (int i=0; i <= NamePool.MAX_PREFIXES_PER_URI; i++) {
			String result = parser.evaluate("<ns" + i + ":test xmlns:ns" + i + "=\"http://www.test.com\">NamePoolLimitTest</ns" + i + ":test>");
			assertEquals("test", result);
		}
	}
}
