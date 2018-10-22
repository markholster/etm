package com.jecstar.etm.server.core.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Utility class for date manipulations.
 *
 * @author Mark Holster
 */
public class DateUtils {

    public static DateTimeFormatter getIndexPerDayFormatter() {
        return new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4)
                .appendLiteral("-")
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .appendLiteral("-")
                .appendValue(ChronoField.DAY_OF_MONTH, 2).toFormatter().withZone(ZoneId.of("UTC"));
    }

}
