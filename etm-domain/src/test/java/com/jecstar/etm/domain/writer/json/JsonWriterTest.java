package com.jecstar.etm.domain.writer.json;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonWriterTest {

	@Test
	public void testJsonEscaping() {
		JsonWriter writer = new JsonWriter();
		assertEquals("Test", writer.escapeToJson("Test", false));
		assertEquals("Te\\\"st", writer.escapeToJson("Te\"st", false));
		assertEquals("Test\\u0017", writer.escapeToJson("Test" + ((char) 23), false));
	}

}
