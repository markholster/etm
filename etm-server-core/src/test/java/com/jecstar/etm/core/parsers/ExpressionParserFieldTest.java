package com.jecstar.etm.core.parsers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.jecstar.etm.server.core.domain.parser.ExpressionParserField;

/**
 * Test class for the <code>ExpressionParserField</code> class.
 * 
 * @author Mark Holster
 */
public class ExpressionParserFieldTest {
	
	@Test
	public void testGetCollectionKeyName() {
		String testKey = "test_key";
		String fullKey = ExpressionParserField.CORRELATION_DATA.getJsonTag() + testKey;
		assertEquals(testKey, ExpressionParserField.CORRELATION_DATA.getCollectionKeyName(fullKey));
	}
}
