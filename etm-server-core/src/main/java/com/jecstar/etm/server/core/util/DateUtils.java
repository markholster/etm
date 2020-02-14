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

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for date manipulations.
 *
 * @author Mark Holster
 */
public class DateUtils {

    private static final DateStringParser DAY_PARSER = new DateStringParser("yyyy-MM-dd");
    private static final DateStringParser HOUR_PARSER = new DateStringParser("yyyy-MM-dd'T'HH");
    private static final DateStringParser MINUTE_PARSER = new DateStringParser("yyyy-MM-dd'T'HH:mm");
    private static final DateStringParser SECOND_PARSER = new DateStringParser("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateStringParser MILLISECOND_PARSER = new DateStringParser("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public static DateTimeFormatter getIndexPerDayFormatter() {
        return new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4)
                .appendLiteral("-")
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .appendLiteral("-")
                .appendValue(ChronoField.DAY_OF_MONTH, 2).toFormatter().withZone(ZoneId.of("UTC"));
    }

    public static Instant parseDateString(String dateString, ZoneId zoneId, boolean normalizeToStart) {
        if (dateString == null || dateString.length() == 0) {
            return null;
        }
        if (!Character.isDigit(dateString.charAt(0))) {
            return null;
        }
        Instant instant = MILLISECOND_PARSER.parse(dateString, zoneId, normalizeToStart);
        if (instant == null) {
            instant = SECOND_PARSER.parse(dateString, zoneId, normalizeToStart);
        }
        if (instant == null) {
            instant = MINUTE_PARSER.parse(dateString, zoneId, normalizeToStart);
        }
        if (instant == null) {
            instant = HOUR_PARSER.parse(dateString, zoneId, normalizeToStart);
        }
        if (instant == null) {
            instant = DAY_PARSER.parse(dateString, zoneId, normalizeToStart);
        }
        return instant;
    }

    private static class DateStringParser {

        private final DateTimeFormatter formatter;
        private final Set<TemporalField> fields = new HashSet<>();

        public DateStringParser(String format) {
            this.formatter = new DateTimeFormatterBuilder().appendPattern(format).appendOptional(new DateTimeFormatterBuilder().appendPattern("XXX").toFormatter()).toFormatter();
            if (format.contains("yyyy")) {
                this.fields.add(ChronoField.YEAR);
            }
            if (format.contains("MM")) {
                this.fields.add(ChronoField.MONTH_OF_YEAR);
            }
            if (format.contains("dd")) {
                this.fields.add(ChronoField.DAY_OF_MONTH);
            }
            if (format.contains("HH")) {
                this.fields.add(ChronoField.HOUR_OF_DAY);
            }
            if (format.contains("mm")) {
                this.fields.add(ChronoField.MINUTE_OF_HOUR);
            }
            if (format.contains("ss")) {
                this.fields.add(ChronoField.SECOND_OF_MINUTE);
            }
            if (format.contains("SSS")) {
                this.fields.add(ChronoField.MILLI_OF_SECOND);
            }
        }

        public Instant parse(String dateString, ZoneId zoneId, boolean setAtStart) {
            try {
                TemporalAccessor temporalAccessor;
                if (dateString.contains("T") && !endsWithOffset(dateString)) {
                    temporalAccessor = this.formatter.withZone(zoneId).parseBest(dateString, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
                } else {
                    temporalAccessor = this.formatter.parseBest(dateString, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
                }
                if (temporalAccessor instanceof ZonedDateTime) {
                    ZonedDateTime zonedDateTime = (ZonedDateTime) temporalAccessor;
                    return Instant.from(normalize(zonedDateTime, setAtStart));
                } else if (temporalAccessor instanceof LocalDateTime) {
                    LocalDateTime localDateTime = (LocalDateTime) temporalAccessor;
                    return Instant.from(normalize(localDateTime.atZone(zoneId), setAtStart));
                }
                LocalDate localDate = (LocalDate) temporalAccessor;
                LocalDateTime localDateTime = LocalDateTime.of(localDate, setAtStart ? LocalTime.MIN : LocalTime.MAX);
                return Instant.from(normalize(localDateTime.atZone(zoneId), setAtStart));
            } catch (DateTimeException e) {
                return null;
            }
        }

        private boolean endsWithOffset(String dateString) {
            return dateString.endsWith("Z") || dateString.matches(".*([+-]([01]?[0-9]|2[0-3]):[0-5][0-9])");
        }

        @SuppressWarnings("unchecked")
        private <T extends Temporal> T normalize(T temporal, boolean toStart) {
            if (!this.fields.contains(ChronoField.NANO_OF_SECOND) && temporal.isSupported(ChronoField.NANO_OF_SECOND)) {
                temporal = (T) temporal.with(ChronoField.NANO_OF_SECOND, toStart ? 0 : 999);
            }
            if (!this.fields.contains(ChronoField.MILLI_OF_SECOND) && temporal.isSupported(ChronoField.MILLI_OF_SECOND)) {
                temporal = (T) temporal.with(ChronoField.MILLI_OF_SECOND, toStart ? 0 : 999);
            }
            if (!this.fields.contains(ChronoField.SECOND_OF_MINUTE) && temporal.isSupported(ChronoField.SECOND_OF_MINUTE)) {
                temporal = (T) temporal.with(ChronoField.SECOND_OF_MINUTE, toStart ? 0 : 59);
            }
            if (!this.fields.contains(ChronoField.MINUTE_OF_HOUR) && temporal.isSupported(ChronoField.MINUTE_OF_HOUR)) {
                temporal = (T) temporal.with(ChronoField.MINUTE_OF_HOUR, toStart ? 0 : 59);
            }
            if (!this.fields.contains(ChronoField.HOUR_OF_DAY) && temporal.isSupported(ChronoField.HOUR_OF_DAY)) {
                temporal = (T) temporal.with(ChronoField.HOUR_OF_DAY, toStart ? 0 : 23);
            }
            return temporal;
        }

    }

}
