package com.jecstar.etm.server.core.util;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the <code>ObjectUtils</code> class.
 * 
 * @author Mark Holster
 */
public class ObjectUtilsTest {

	/**
	 * Test equality of objects.
	 */
	@Test
	public void testEqualsNullProof() {
		assertFalse(ObjectUtils.equalsNullProof(null, "a"));
		assertFalse(ObjectUtils.equalsNullProof("a", null));
		assertFalse(ObjectUtils.equalsNullProof("a", "A"));
		assertTrue(ObjectUtils.equalsNullProof(null, null));
		assertFalse(ObjectUtils.equalsNullProof(null, null, false));
		assertTrue(ObjectUtils.equalsNullProof("a", "a"));
	}
}
