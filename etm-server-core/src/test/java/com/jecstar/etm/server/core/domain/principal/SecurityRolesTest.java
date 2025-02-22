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

package com.jecstar.etm.server.core.domain.principal;


import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Test class for the <code>SecurityRoles</code> class.
 */
public class SecurityRolesTest {

    /**
     * Test if all declared fields are also declared in the ALL_ROLES_ARRAY field.
     */
    @Test
    public void testAllFieldsInAllRolesArray() {
        // The number of fields minus 2 (ALL_ROLES, ALL_ROLES_ARRAY & ALL_READ_WRITE_ACCESS_ROLES) should be the same.
        assertEquals(SecurityRoles.class.getDeclaredFields().length - 3, SecurityRoles.ALL_ROLES_ARRAY.length);
    }

    /**
     * Test if the order of the ALL_ROLES_ARRAY is ascending.
     */
    @Test
    public void testAllRolesArrayOrder() {
        String[] old = new String[SecurityRoles.ALL_ROLES_ARRAY.length];
        System.arraycopy(SecurityRoles.ALL_ROLES_ARRAY, 0, old, 0, SecurityRoles.ALL_ROLES_ARRAY.length);
        Arrays.sort(SecurityRoles.ALL_ROLES_ARRAY);
    }

    /**
     * Test the equality of the ALL_ROLES_ARRAY and ALL_ROLES fields.
     */
    @Test
    public void testAllRolesArrayVsAllRolesEquality() {
        assertEquals("{\"" + Arrays.stream(SecurityRoles.ALL_ROLES_ARRAY).collect(Collectors.joining("\",\"")) + "\"}", SecurityRoles.ALL_ROLES);
    }


}
