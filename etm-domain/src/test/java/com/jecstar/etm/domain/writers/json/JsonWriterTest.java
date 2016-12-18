package com.jecstar.etm.domain.writers.json;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JsonWriterTest {

	@Test
	public void testJsonEscaping() {
		JsonWriter writer = new JsonWriter();
		assertEquals("Test", writer.escapeToJson("Test", false));
		assertEquals("Te\\\"st", writer.escapeToJson("Te\"st", false));
		assertEquals("Test\\u0017", writer.escapeToJson("Test" + ((char) 23), false));
	}

}
