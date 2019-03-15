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
        // Zone should only be taken into consideration when given time has no offset.
        assertEquals(JAN_FIRST_UTC, DateUtils.parseDateString("2019-01-01T02+02:00", ZoneId.of("+05:00"), true).toEpochMilli());
    }

}
