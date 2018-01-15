package com.jecstar.etm.server.core.domain.parsers;


import com.jecstar.etm.server.core.domain.parser.JsonPathExpressionParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for the <code>JsonPathExpressionParser</code> class.
 * 
 * @author Mark Holster
 */
public class JsonExpressionParserTest {

	private final String json = "{ \"store\": {\n" +
			"    \"book\": [ \n" + 
			"      { \"category\": \"reference\",\n" + 
			"        \"author\": \"Nigel Rees\",\n" + 
			"        \"title\": \"Sayings of the Century\",\n" + 
			"        \"price\": 8.95\n" + 
			"      },\n" + 
			"      { \"category\": \"fiction\",\n" + 
			"        \"author\": \"Evelyn Waugh\",\n" + 
			"        \"title\": \"Sword of Honour\",\n" + 
			"        \"price\": 12.99\n" + 
			"      },\n" + 
			"      { \"category\": \"fiction\",\n" + 
			"        \"author\": \"Herman Melville\",\n" + 
			"        \"title\": \"Moby Dick\",\n" + 
			"        \"isbn\": \"0-553-21311-3\",\n" + 
			"        \"price\": 8.99\n" + 
			"      },\n" + 
			"      { \"category\": \"fiction\",\n" + 
			"        \"author\": \"J. R. R. Tolkien\",\n" + 
			"        \"title\": \"The Lord of the Rings\",\n" + 
			"        \"isbn\": \"0-395-19395-8\",\n" + 
			"        \"price\": 22.99\n" + 
			"      }\n" + 
			"    ],\n" + 
			"    \"bicycle\": {\n" + 
			"      \"color\": \"red\",\n" + 
			"      \"price\": 19.95\n" + 
			"    }\n" + 
			"  }\n" + 
			"}";
	
	@Test
	public void testEvaluate() {
		JsonPathExpressionParser parser = new JsonPathExpressionParser("test", "$.store.book[0].author");
		assertEquals("Nigel Rees", parser.evaluate(json));
		
	}
}
