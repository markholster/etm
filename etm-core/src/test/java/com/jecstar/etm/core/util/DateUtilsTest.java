package com.jecstar.etm.core.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test class for the <code>DateUtils</code> class.
 * 
 * @author Mark Holster
 */
public class DateUtilsTest {
	
	/**
	 * Test normalization of time.
	 */
	@Test
	public void testNormalizeTime() {
		long time = System.currentTimeMillis();
		long normalizedTime = DateUtils.normalizeTime(time, 10);
		assertEquals(0, normalizedTime % 10);
		normalizedTime = DateUtils.normalizeTime(time, 100);
		assertEquals(0, normalizedTime % 100);
		normalizedTime = DateUtils.normalizeTime(time, 1000);
		assertEquals(0, normalizedTime % 1000);
	}

}
