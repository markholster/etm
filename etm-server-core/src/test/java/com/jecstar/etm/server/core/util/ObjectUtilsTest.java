/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

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
