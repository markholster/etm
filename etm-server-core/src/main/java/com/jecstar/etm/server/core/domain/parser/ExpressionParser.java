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

package com.jecstar.etm.server.core.domain.parser;

/**
 * Interface for all <code>ExpressionParser</code> instances.
 * <p>
 * An <code>ExpressionParser</code> is capable of extracting data from an <code>TelemetryEvent</code> before it is stored
 * to Elasticsearch. Some <code>ExpressionParser</code>s are capable of replacing content as well.
 */
public interface ExpressionParser extends AutoCloseable {

    /**
     * Gives the name of the <code>ExpressionParser</code>.
     *
     * @return The name of the <code>ExpressionParser</code>/
     */
    String getName();

    /**
     * Evaluates this <code>ExpressionParser</code> against the given content.
     * @param content The content to evaluate.
     * @return The extracted value or <code>null</code> when no value could be extracted.
     */
    String evaluate(String content);

    /**
     * Boolean indicating this <code>ExpressionParser</code> is capable of replacing content.
     *
     * @return <code>true</code> when this <code>ExpressionParser</code> is capable of replacing content.
     */
    boolean isCapableOfReplacing();

    /**
     * Replaces some (or all) parts of the given content.
     *
     * @param content     The content to evaluate and replace.
     * @param replacement The replacement.
     * @param allValues   <code>true</code> when all occurrences should be replaced. <code>false</code> when only the first
     *                    occurrence should be replaced.
     * @return The content with the replaced values.
     * @throws UnsupportedOperationException when {@link #isCapableOfReplacing()} returns <code>false</code>.
     */
    String replace(String content, String replacement, boolean allValues);
}
