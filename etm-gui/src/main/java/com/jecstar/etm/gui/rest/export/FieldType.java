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

package com.jecstar.etm.gui.rest.export;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public enum FieldType {

    ISO_UTC_TIMESTAMP() {
        @SuppressWarnings("unchecked")
        @Override
        String formatValue(Object value, ZoneId zoneId) {
            if (value == null) {
                return null;
            }
            Collection<Object> values = new ArrayList<>();
            if (value instanceof Collection<?>) {
                values = (Collection<Object>) value;
            } else {
                values.add(value);
            }
            return values.stream().map(f -> {
                if (f instanceof Number) {
                    return ISO_UTC_FORMATTER.format(Instant.ofEpochMilli(((Number) f).longValue()));
                }
                return f.toString();
            }).collect(Collectors.joining(", "));
        }
    }, ISO_TIMESTAMP() {
        @SuppressWarnings("unchecked")
        @Override
        String formatValue(Object value, ZoneId zoneId) {
            if (value == null) {
                return null;
            }
            Collection<Object> values = new ArrayList<>();
            if (value instanceof Collection<?>) {
                values = (Collection<Object>) value;
            } else {
                values.add(value);
            }
            return values.stream().map(f -> {
                if (f instanceof Number) {
                    return ISO_FORMATTER.withZone(zoneId).format(Instant.ofEpochMilli(((Number) f).longValue()));
                }
                return f.toString();
            }).collect(Collectors.joining(", "));
        }
    }, PLAIN() {
        @SuppressWarnings("unchecked")
        @Override
        String formatValue(Object value, ZoneId zoneId) {
            if (value == null) {
                return null;
            }
            Collection<Object> values = new ArrayList<>();
            if (value instanceof Collection<?>) {
                values = (Collection<Object>) value;
            } else {
                values.add(value);
            }
            return values.stream().map(Object::toString).collect(Collectors.joining(", "));
        }
    };

    private static final DateTimeFormatter ISO_UTC_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    /**
     * Format the gives value to a <code>String</code>.
     *
     * @param value  The value to be formatted. This can also be a <code>Collection</code> instance.
     * @param zoneId The <code>ZoneId</code> to format date instances.
     * @return The formatted value.
     */
    abstract String formatValue(Object value, ZoneId zoneId);

    public static FieldType fromJsonValue(String jsonValue) {
        if ("PLAIN".equals(jsonValue)) {
            return PLAIN;
        } else if ("ISOUTCTIMESTAMP".equals(jsonValue)) {
            return ISO_UTC_TIMESTAMP;
        } else if ("ISOTIMESTAMP".equals(jsonValue)) {
            return ISO_TIMESTAMP;
        }
        return null;
    }

}
