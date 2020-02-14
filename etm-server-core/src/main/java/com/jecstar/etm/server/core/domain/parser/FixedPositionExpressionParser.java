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

public class FixedPositionExpressionParser extends AbstractExpressionParser {

    private final Integer startIx;
    private final Integer endIx;
    private final Integer line;

    public FixedPositionExpressionParser(final String name, final Integer line, final Integer startIx, final Integer endIx) {
        super(name);
        this.line = line;
        this.startIx = startIx;
        this.endIx = endIx;
    }

    @Override
    public String evaluate(String content) {
        if (content == null) {
            return null;
        }
        String line;
        if (this.line != null) {
            String lines[] = content.split("\n");
            if (lines.length <= this.line) {
                return null;
            }
            line = lines[this.line];
        } else {
            line = content;
        }
        if (this.startIx != null && (this.startIx >= line.length() || this.startIx < 0)) {
            return null;
        }
        if (this.endIx != null && (this.endIx < 1 || this.endIx > line.length())) {
            return null;
        }
        if (this.startIx == null && this.endIx == null) {
            return null;
        }
        if (this.startIx != null && this.endIx == null) {
            return line.substring(this.startIx);
        } else if (this.startIx == null && this.endIx != null) {
            return line.substring(0, this.endIx);
        } else {
            return line.substring(this.startIx, this.endIx);
        }
    }

    public Integer getStartIx() {
        return this.startIx;
    }

    public Integer getEndIx() {
        return this.endIx;
    }

    public Integer getLine() {
        return this.line;
    }

}
