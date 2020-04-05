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

package com.jecstar.etm.domain.writer.json;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Helper class for creating json data.
 */
public class JsonBuilder {

    private static final char[] HEX_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final int NO_ESCAPE = 0;
    private static final int STANDARD_ESCAPE = -1;
    private static final int[] ESCAPE_TABLE;

    static {
        int[] table = new int[128];
        for (int i = 0; i < 32; ++i) {
            table[i] = STANDARD_ESCAPE;
        }
        table['"'] = '"';
        table['\\'] = '\\';
        table[0x08] = 'b';
        table[0x09] = 't';
        table[0x0C] = 'f';
        table[0x0A] = 'n';
        table[0x0D] = 'r';
        ESCAPE_TABLE = table;
    }

    /**
     * The writing context.
     */
    private Context context = new Context();

    /**
     * The buffer that holds the current added data.
     */
    private StringBuilder buffer = new StringBuilder();

    /**
     * Starts an object. This method will write a <code>{</code> and can be called as the first method, or within the {@link Context#IN_ARRAY} context.
     *
     * @return This instance for chaining.
     */
    public JsonBuilder startObject() {
        if (!context.isInRoot() && !context.isInArray()) {
            throw new IllegalStateException("Context is not in root or array");
        }
        if (context.hasName()) {
            buffer.append(",");
        }
        context.subContext(Context.IN_OBJECT, null);
        buffer.append("{");
        return this;
    }

    /**
     * Starts a new object with a name. This method will write <code>"&lt;name&gt;": {</code> and can be called within the {@link Context#IN_OBJECT} context.
     *
     * @return This instance for chaining.
     */
    public JsonBuilder startObject(String name) {
        if (!context.isInObject()) {
            throw new IllegalStateException("Context is not in object");
        }
        if (context.hasName()) {
            buffer.append(",");
        }
        context.name = name;
        context.subContext(Context.IN_OBJECT, null);
        buffer.append(escapeToJson(name, true));
        buffer.append(":{");
        return this;
    }

    /**
     * Ends an object by writing <code>}</code>. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @return This instance for chaining.
     */
    public JsonBuilder endObject() {
        if (!context.isInObject()) {
            throw new IllegalStateException("Context is not in object");
        }
        context.endCurrentContext();
        buffer.append("}");
        return this;
    }

    /**
     * Starts a new array with a name. This method will write <code>"&lt;name&gt;": [</code> and can be called within the {@link Context#IN_OBJECT} context.
     *
     * @return This instance for chaining.
     */
    public JsonBuilder startArray(String name) {
        if (!context.isInObject()) {
            throw new IllegalStateException("Context is not in object");
        }
        if (context.hasName()) {
            buffer.append(",");
        }
        context.name = name;
        context.subContext(Context.IN_ARRAY, null);
        buffer.append(escapeToJson(name, true));
        buffer.append(":[");
        return this;
    }

    /**
     * Ends an array by writing <code>]</code>. This method can be called within the {@link Context#IN_ARRAY} context.
     *
     * @return This instance for chaining.
     */
    public JsonBuilder endArray() {
        if (!context.isInArray()) {
            throw new IllegalStateException("Context is not in array");
        }
        context.endCurrentContext();
        buffer.append("]");
        return this;
    }

    /**
     * This method will write a given object as string. It also tries to determine the object type and write it as that type as well. In that case the given name will be appended with <code>_as_&lt;type&gt;</code>. This method can be called within the {@link Context#IN_ARRAY} context.
     *
     * @param name  The name that should be used as the json name of the field.
     * @param value The value of the field.
     * @return This instance for chaining.
     */
    public JsonBuilder fieldAsString(String name, Object value) {
        if (value == null) {
            fieldWithSupplier(name, () -> escapeToJson("null", true), false);
        } else if (value instanceof Number) {
            fieldWithSupplier(name, () -> escapeToJson(value.toString(), true), false);
            fieldWithSupplier(name + "_as_number", value::toString, false);
        } else if (value instanceof Boolean) {
            fieldWithSupplier(name, () -> escapeToJson(value.toString(), true), false);
            fieldWithSupplier(name + "_as_boolean", value::toString, false);
        } else if (value instanceof Date) {
            fieldWithSupplier(name, () -> escapeToJson(value.toString(), true), false);
            fieldWithSupplier(name + "_as_date", () -> String.valueOf(((Date) value).getTime()), false);
        } else {
            fieldWithSupplier(name, () -> escapeToJson(value.toString(), true), false);
        }
        return this;
    }

    /**
     * Adds a String field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name  The name of the field.
     * @param value The value of the field.
     * @return This instance for chaining.
     */
    public JsonBuilder field(String name, String value) {
        return field(name, value, false);
    }

    /**
     * Adds a String field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name          The name of the field.
     * @param value         The value of the field.
     * @param writeWhenNull <code>true</code> when a null value should be written when the given value is null. <code>false</code> if the field should be skipped when te value is null.
     * @return This instance for chaining.
     */
    public JsonBuilder field(String name, String value, boolean writeWhenNull) {
        fieldWithSupplier(name, () -> value == null ? null : escapeToJson(value, true), writeWhenNull);
        return this;
    }

    /**
     * Adds an array of Strings as field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name   The name of the array field.
     * @param values The values that are added as array.
     * @return This instance for chaining
     */
    public JsonBuilder field(String name, String... values) {
        arrayFieldWithSupplier(name, () -> (values == null) ? null : Arrays.stream(values).map(v -> escapeToJson(v, true)).toArray(String[]::new), false);
        return this;
    }

    /**
     * Adds an Integer field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name  The name of the field.
     * @param value The value of the field.
     * @return This instance for chaining.
     */
    public JsonBuilder field(String name, Integer value) {
        return field(name, value, false);
    }

    /**
     * Adds an Integer field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name          The name of the field.
     * @param value         The value of the field.
     * @param writeWhenNull <code>true</code> when a null value should be written when the given value is null. <code>false</code> if the field should be skipped when te value is null.
     * @return This instance for chaining.
     */
    public JsonBuilder field(String name, Integer value, boolean writeWhenNull) {
        fieldWithSupplier(name, () -> value == null ? null : value.toString(), writeWhenNull);
        return this;
    }

    /**
     * Adds an array of Integers as field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name   The name of the array field.
     * @param values The values that are added as array.
     * @return This instance for chaining
     */
    public JsonBuilder field(String name, Integer... values) {
        arrayFieldWithSupplier(name, values == null ? null : () -> Arrays.stream(values).map(Objects::toString).toArray(String[]::new), false);
        return this;
    }

    /**
     * Adds a Long field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name  The name of the field.
     * @param value The value of the field.
     * @return This instance for chaining.
     */
    public JsonBuilder field(String name, Long value) {
        return field(name, value, false);
    }

    /**
     * Adds a Long field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name          The name of the field.
     * @param value         The value of the field.
     * @param writeWhenNull <code>true</code> when a null value should be written when the given value is null. <code>false</code> if the field should be skipped when te value is null.
     * @return This instance for chaining.
     */
    public JsonBuilder field(String name, Long value, boolean writeWhenNull) {
        fieldWithSupplier(name, () -> value == null ? null : value.toString(), writeWhenNull);
        return this;
    }

    /**
     * Adds an array of Longs as field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name   The name of the array field.
     * @param values The values that are added as array.
     * @return This instance for chaining
     */
    public JsonBuilder field(String name, Long... values) {
        arrayFieldWithSupplier(name, values == null ? null : () -> Arrays.stream(values).map(Objects::toString).toArray(String[]::new), false);
        return this;
    }

    /**
     * Adds a Double field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name  The name of the field.
     * @param value The value of the field.
     * @return This instance for chaining.
     */
    public JsonBuilder field(String name, Double value) {
        return field(name, value, false);
    }

    /**
     * Adds a Double field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name          The name of the field.
     * @param value         The value of the field.
     * @param writeWhenNull <code>true</code> when a null value should be written when the given value is null. <code>false</code> if the field should be skipped when te value is null.
     * @return This instance for chaining.
     */
    public JsonBuilder field(String name, Double value, boolean writeWhenNull) {
        fieldWithSupplier(name, () -> (value == null || value.isNaN()) ? null : value.toString(), writeWhenNull);
        return this;
    }

    /**
     * Adds an array of Doubles as field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name   The name of the array field.
     * @param values The values that are added as array.
     * @return This instance for chaining
     */
    public JsonBuilder field(String name, Double... values) {
        arrayFieldWithSupplier(name, values == null ? null : () -> Arrays.stream(values).map(Objects::toString).toArray(String[]::new), false);
        return this;
    }

    /**
     * Adds a Float field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name  The name of the field.
     * @param value The value of the field.
     * @return This instance for chaining.
     */
    public JsonBuilder field(String name, Float value) {
        return field(name, value, false);
    }

    /**
     * Adds a Float field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name          The name of the field.
     * @param value         The value of the field.
     * @param writeWhenNull <code>true</code> when a null value should be written when the given value is null. <code>false</code> if the field should be skipped when te value is null.
     * @return This instance for chaining.
     */
    public JsonBuilder field(String name, Float value, boolean writeWhenNull) {
        fieldWithSupplier(name, () -> (value == null || value.isNaN()) ? null : value.toString(), writeWhenNull);
        return this;
    }

    /**
     * Adds an array of Floats as field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name   The name of the array field.
     * @param values The values that are added as array.
     * @return This instance for chaining
     */
    public JsonBuilder field(String name, Float... values) {
        arrayFieldWithSupplier(name, values == null ? null : () -> Arrays.stream(values).map(Objects::toString).toArray(String[]::new), false);
        return this;
    }

    /**
     * Adds a Boolean field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name  The name of the field.
     * @param value The value of the field.
     * @return This instance for chaining.
     */
    public JsonBuilder field(String name, Boolean value) {
        return field(name, value, false);
    }

    /**
     * Adds a Boolean field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name          The name of the field.
     * @param value         The value of the field.
     * @param writeWhenNull <code>true</code> when a null value should be written when the given value is null. <code>false</code> if the field should be skipped when te value is null.
     * @return This instance for chaining.
     */
    public JsonBuilder field(String name, Boolean value, boolean writeWhenNull) {
        fieldWithSupplier(name, () -> value == null ? null : value.toString(), writeWhenNull);
        return this;
    }

    /**
     * Adds an array of Booleans as field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name   The name of the array field.
     * @param values The values that are added as array.
     * @return This instance for chaining
     */
    public JsonBuilder field(String name, Boolean... values) {
        arrayFieldWithSupplier(name, values == null ? null : () -> Arrays.stream(values).map(Objects::toString).toArray(String[]::new), false);
        return this;
    }

    /**
     * Adds an Instant field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name  The name of the field.
     * @param value The value of the field.
     * @return This instance for chaining.
     */
    public JsonBuilder field(String name, Instant value) {
        return field(name, value, false);
    }

    /**
     * Adds an Instant field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name          The name of the field.
     * @param value         The value of the field.
     * @param writeWhenNull <code>true</code> when a null value should be written when the given value is null. <code>false</code> if the field should be skipped when te value is null.
     * @return This instance for chaining.
     */
    public JsonBuilder field(String name, Instant value, boolean writeWhenNull) {
        return field(name, value == null ? null : value.toEpochMilli(), writeWhenNull);
    }

    /**
     * Adds an array of Instant as field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name   The name of the array field.
     * @param values The values that are added as array.
     * @return This instance for chaining
     */
    public JsonBuilder field(String name, Instant... values) {
        arrayFieldWithSupplier(name, values == null ? null : () -> Arrays.stream(values).map(Instant::toEpochMilli).map(Objects::toString).toArray(String[]::new), false);
        return this;
    }

    /**
     * Adds a Collection as field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name   The name of the array field.
     * @param values The values that are added as array.
     * @return This instance for chaining
     */
    public JsonBuilder field(String name, Collection<?> values) {
        if (values == null) {
            return field(name, (String) null);
        } else if (values.isEmpty()) {
            return field(name, new String[0]);
        }
        Object object = values.iterator().next();
        if (object instanceof String) {
            return field(name, values.toArray(new String[0]));
        } else if (object instanceof Boolean) {
            return field(name, values.toArray(new Boolean[0]));
        } else if (object instanceof Integer) {
            return field(name, values.toArray(new Integer[0]));
        } else if (object instanceof Long) {
            return field(name, values.toArray(new Long[0]));
        } else if (object instanceof Float) {
            return field(name, values.toArray(new Float[0]));
        } else if (object instanceof Double) {
            return field(name, values.toArray(new Double[0]));
        } else if (object instanceof Instant) {
            return field(name, values.toArray(new Instant[0]));
        }
        return field(name, values.stream().map(Object::toString).toArray(String[]::new));
    }

    /**
     * Adds an InetAddress field to a json object. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param hostAddressName The name of the field that store the host address part of the <code>InetAddress</code>.
     * @param hostNameName    The name of the field that store the hostname part of the <code>InetAddress</code>.
     * @param value           The value of the field.
     * @return This instance for chaining.
     */
    public JsonBuilder field(String hostAddressName, String hostNameName, InetAddress value) {
        fieldWithSupplier(hostAddressName, () -> value == null ? null : escapeToJson(value.getHostAddress(), true), false);
        fieldWithSupplier(hostNameName, () -> value == null ? null : escapeToJson(value.getHostName(), true), false);
        return this;
    }

    /**
     * Adds a field with a raw value to a json object. The value will be added to the buffer unchanged. This method can be called within the {@link Context#IN_OBJECT} context.
     *
     * @param name  The name of the field.
     * @param value The value of the field.
     * @return This instance for chaining.
     */
    public JsonBuilder rawField(String name, String value) {
        fieldWithSupplier(name, () -> value, false);
        return this;
    }

    /**
     * Adds an element with a raw values to a json object. The value will be added to the buffer unchanged and needs to be a valid json element.
     * This method can be called within the {@link Context#IN_ARRAY} context.
     *
     * @param object The object as raw json.
     * @return This instance for chaining.
     */
    public JsonBuilder rawElement(String object) {
        if (!context.isInArray()) {
            throw new IllegalStateException("Context is not in array");
        }
        if (context.hasName()) {
            buffer.append(",");
        }
        // Set a custom name so the next object is prefixed with a comma.
        context.name = "AFTER_RAW_ELEMENT";
        buffer.append(object);
        return this;
    }

    /**
     * Generic method for adding a json field with a given value.
     *
     * @param name          The name of the field.
     * @param supplier      The supplier that should supply the fully formatted/escaped json value.
     * @param writeWhenNull <code>true</code> when a null value should be written in case the supplier returns <code>null</code>.
     */
    private void fieldWithSupplier(String name, Supplier<String> supplier, boolean writeWhenNull) {
        if (!context.isInObject()) {
            throw new IllegalStateException("Context is not in object");
        }
        var value = supplier.get();
        if (value == null && !writeWhenNull) {
            return;
        } else if (value == null) {
            value = "null";
        }
        if (context.hasName()) {
            buffer.append(",");
        }
        context.name = name;
        buffer.append(escapeToJson(name, true));
        buffer.append(":");
        buffer.append(value);
    }

    /**
     * Generic method for adding a json field with a given array.
     *
     * @param name          The name of the field.
     * @param supplier      The supplier that should supply the fully formatted/escaped json values as <code>String</code> array.
     * @param writeWhenNull <code>true</code> when a null value should be written in case the supplier returns <code>null</code>.
     */
    private void arrayFieldWithSupplier(String name, Supplier<String[]> supplier, boolean writeWhenNull) {
        if (!context.isInObject()) {
            throw new IllegalStateException("Context is not in object");
        }
        var values = supplier.get();
        if ((values == null) && !writeWhenNull) {
            return;
        } else if (values == null) {
            values = new String[0];
        }
        if (context.hasName()) {
            buffer.append(",");
        }
        context.name = name;
        buffer.append(escapeToJson(name, true));
        buffer.append(":[");
        buffer.append(Arrays.stream(values).collect(Collectors.joining(",")));
        buffer.append("]");
    }

    /**
     * Build and return the json data as String.
     *
     * @return The added fields, objects and arrays as Json String.
     */
    public String build() {
        return buffer.toString();
    }

    /**
     * Escape a String to a json string.
     *
     * @param value The value to escape.
     * @param quote <code>true</code> when the value should be quoted, <code>false</code> otherwise.
     * @return The json escaped value.
     */
    public static String escapeToJson(String value, boolean quote) {
        if (value == null) {
            return "null";
        }
        int maxLength = value.length() * 6;
        if (quote) {
            maxLength += 2;
        }
        char[] outputBuffer = new char[maxLength];
        final int escLen = ESCAPE_TABLE.length;
        int outputPointer = 0;
        if (quote) {
            outputBuffer[outputPointer++] = '"';
        }
        conversion_loop:
        for (int i = 0; i < value.length(); i++) {
            while (true) {
                char c = value.charAt(i);
                if (c < escLen && ESCAPE_TABLE[c] != NO_ESCAPE) {
                    break;
                }
                outputBuffer[outputPointer++] = c;
                if (++i >= value.length()) {
                    break conversion_loop;
                }
            }
            char c = value.charAt(i);
            outputPointer = appendCharacterEscape(outputBuffer, outputPointer, c, ESCAPE_TABLE[c]);
        }
        if (quote) {
            outputBuffer[outputPointer++] = '"';
        }
        char[] result = new char[outputPointer];
        System.arraycopy(outputBuffer, 0, result, 0, outputPointer);
        return new String(result);
    }

    private static int appendCharacterEscape(char[] outputBuffer, int outputPointer, char ch, int escCode) {
        if (escCode > NO_ESCAPE) {
            outputBuffer[outputPointer++] = '\\';
            outputBuffer[outputPointer++] = (char) escCode;
            return outputPointer;
        }
        if (escCode == STANDARD_ESCAPE) {
            outputBuffer[outputPointer++] = '\\';
            outputBuffer[outputPointer++] = 'u';
            // We know it's a control char, so only the last 2 chars are non-0
            if (ch > 0xFF) { // beyond 8 bytes
                int hi = (ch >> 8) & 0xFF;
                outputBuffer[outputPointer++] = HEX_CHARS[hi >> 4];
                outputBuffer[outputPointer++] = HEX_CHARS[hi & 0xF];
                ch &= 0xFF;
            } else {
                outputBuffer[outputPointer++] = '0';
                outputBuffer[outputPointer++] = '0';
            }
            outputBuffer[outputPointer++] = HEX_CHARS[ch >> 4];
            outputBuffer[outputPointer++] = HEX_CHARS[ch & 0xF];
            return outputPointer;
        }
        outputBuffer[outputPointer++] = (char) escCode;
        return outputPointer;
    }

    /**
     * Class representing the current context of the builder.
     */
    private class Context {
        public static final int IN_ROOT = 0;
        public static final int IN_OBJECT = 1;
        public static final int IN_ARRAY = 2;

        /**
         * The current location of the pointer.
         */
        public int location;
        /**
         * The current fieldname.
         */
        public String name;

        /**
         * The parent context.
         */
        private Context parent;

        /**
         * Determine if the current context is within the root object.
         *
         * @return <code>true</code> when the pointer is within the root object, <code>false</code> otherwise.
         */
        public boolean isInRoot() {
            return this.location == IN_ROOT;
        }

        /**
         * Determine if the current context is within an object.
         *
         * @return <code>true</code> when the pointer is within an object, <code>false</code> otherwise.
         */
        public boolean isInObject() {
            return this.location == IN_OBJECT;
        }

        /**
         * Determine if the current context is within an array.
         *
         * @return <code>true</code> when the pointer is within an array, <code>false</code> otherwise.
         */
        public boolean isInArray() {
            return this.location == IN_ARRAY;
        }

        /**
         * Determines if the current context has a name. This will be the case when a field is added to an object or array.
         *
         * @return <code>true</code> when the current pointer has a name, <code>false</code> otherwise.
         */
        public boolean hasName() {
            return this.name != null;
        }

        /**
         * Add a new context. A new context will be created and the current context will be the parent context of the new context.
         *
         * @param location The location of the new context.
         * @param name     The name of the new context.
         */
        public void subContext(int location, String name) {
            Context context = new Context();
            context.location = location;
            context.name = name;
            context.parent = this;
            JsonBuilder.this.context = context;
        }

        /**
         * End the current context and set the parent context as current when available.
         */
        public void endCurrentContext() {
            if (this.parent != null) {
                if (this.parent.name == null) {
                    // Set a dummy name when we end a state (array or object) so the next field will prepend a comma.
                    this.parent.name = "ENDED_CHILD";
                }
                JsonBuilder.this.context = this.parent;
            }
        }
    }

}
