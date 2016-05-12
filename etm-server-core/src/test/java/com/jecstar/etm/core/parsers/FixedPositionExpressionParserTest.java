package com.jecstar.etm.core.parsers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test class for the <code>FixedPositionExpressionParser</code> class.
 * 
 * @author Mark Holster
 */
public class FixedPositionExpressionParserTest {
	
	/**
	 * Test evaluating the parser with multiple lines.
	 */
	@Test
	public void testEvaluateMultipleLines() {
		ExpressionParser parser = new FixedPositionExpressionParser(1, 0, 2); 
		String text = "This is a test\r\non 2 lines.";
		assertEquals("on", parser.evaluate(text));
	}
	
	/**
	 * Test evaluating the parser with no line number.
	 */
	@Test
	public void testEvaluateNoLine() {
		ExpressionParser parser = new FixedPositionExpressionParser(null, 0, 2); 
		String text = "This is a test\r\non 2 lines.";
		assertEquals("Th", parser.evaluate(text));
	}
	
	/**
	 * Test evaluating the parser with no start position.
	 */
	@Test
	public void testEvaluateNoStart() {
		ExpressionParser parser = new FixedPositionExpressionParser(1, null, 2); 
		String text = "This is a test\r\non 2 lines.";
		assertEquals("on", parser.evaluate(text));
	}
	
	
	/**
	 * Test evaluating the parser with no end position.
	 */
	@Test
	public void testEvaluateNoEnd() {
		ExpressionParser parser = new FixedPositionExpressionParser(1, 3, null); 
		String text = "This is a test\r\non 2 lines.";
		assertEquals("2 lines.", parser.evaluate(text));
	}
	
	
}
