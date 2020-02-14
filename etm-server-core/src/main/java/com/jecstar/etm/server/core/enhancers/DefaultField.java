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

package com.jecstar.etm.server.core.enhancers;

import com.jecstar.etm.server.core.domain.parser.ExpressionParser;

import java.util.ArrayList;
import java.util.List;

public class DefaultField {

    public enum WritePolicy {

        OVERWRITE_WHEN_FOUND, ALWAYS_OVERWRITE, WHEN_EMPTY;

        public static WritePolicy safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return WritePolicy.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

    }

    ;


    private final String name;

    private WritePolicy writePolicy = WritePolicy.WHEN_EMPTY;

    private String parsersSource;

    private final List<ExpressionParser> parsers = new ArrayList<>();

    public DefaultField(String name) {
        this.name = name;
    }

    public WritePolicy getWritePolicy() {
        return this.writePolicy;
    }

    public void setWritePolicy(WritePolicy writePolicy) {
        if (writePolicy != null) {
            this.writePolicy = writePolicy;
        }
    }

    public String getParsersSource() {
        return this.parsersSource;
    }

    public void setParsersSource(String parsersSource) {
        this.parsersSource = parsersSource;
    }

    public String getName() {
        return name;
    }

    public List<ExpressionParser> getParsers() {
        return this.parsers;
    }

    public void addParser(ExpressionParser expressionParser) {
        this.parsers.add(expressionParser);
    }

    public void addParsers(List<ExpressionParser> expressionParsers) {
        this.parsers.addAll(expressionParsers);
    }

}
