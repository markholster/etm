package com.jecstar.etm.core.domain.converter.json;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class AbstractJsonConverterTest {

	@Test
	public void testJsonEscaping() {
		AbstractJsonConverter converter = new AbstractJsonConverter() {};
		assertEquals("Test", converter.escapeToJson("Test", false));
		assertEquals("Te\\\"st", converter.escapeToJson("Te\"st", false));
		assertEquals("Test\\u0017", converter.escapeToJson("Test" + ((char) 23), false));
	}
}
