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

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for the <code>DateUtils</code> class.
 *
 * @author Mark Holster
 */
public class DateUtilsTest {

    @Test
    public void testParseDateString() {
        final Long JAN_FIRST_UTC = 1546300800000L;
        assertEquals(JAN_FIRST_UTC, DateUtils.parseDateString("2019-01-01", ZoneId.of("UTC"), true).toEpochMilli());
        assertEquals(JAN_FIRST_UTC, DateUtils.parseDateString("2019-01-01T00", ZoneId.of("UTC"), true).toEpochMilli());
        assertEquals(JAN_FIRST_UTC, DateUtils.parseDateString("2019-01-01T02+02:00", null, true).toEpochMilli());
        assertEquals(JAN_FIRST_UTC, DateUtils.parseDateString("2018-12-31T22-02:00", null, true).toEpochMilli());
        // Zone should only be taken into consideration when given time has no offset.
        assertEquals(JAN_FIRST_UTC, DateUtils.parseDateString("2019-01-01T02+02:00", ZoneId.of("+05:00"), true).toEpochMilli());
    }

}
