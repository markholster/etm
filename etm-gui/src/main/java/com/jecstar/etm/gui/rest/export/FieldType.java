package com.jecstar.etm.gui.rest.export;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public enum FieldType {

    ISO_UTC_TIMESTAMP() {
        @Override
        String formatValue(Object value, ZoneId zoneId) {
            if (value == null) {
                return null;
            }
            if (value instanceof Number) {
                return ISO_UTC_FORMATTER.format(Instant.ofEpochMilli(((Number) value).longValue()));
            }
            return value.toString();
        }
    }, ISO_TIMESTAMP() {
        @Override
        String formatValue(Object value, ZoneId zoneId) {
            if (value == null) {
                return null;
            }
            if (value instanceof Number) {
                return ISO_FORMATTER.withZone(zoneId).format(Instant.ofEpochMilli(((Number) value).longValue()));
            }
            return value.toString();
        }
    }, PLAIN() {
        @Override
        String formatValue(Object value, ZoneId zoneId) {
            if (value == null) {
                return null;
            }
            return value.toString();
        }
    };

    private static final DateTimeFormatter ISO_UTC_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    abstract String formatValue(Object value, ZoneId zoneId);

    public static FieldType fromJsonValue(String jsonValue) {
        if ("plain".equals(jsonValue)) {
            return PLAIN;
        } else if ("isoutctimestamp".equals(jsonValue)) {
            return ISO_UTC_TIMESTAMP;
        } else if ("isotimestamp".equals(jsonValue)) {
            return ISO_TIMESTAMP;
        }
        return null;
    }

}
