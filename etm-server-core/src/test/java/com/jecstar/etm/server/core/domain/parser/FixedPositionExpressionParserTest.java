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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Test class for the <code>FixedPositionExpressionParser</code> class.
 *
 * @author Mark Holster
 */
public class FixedPositionExpressionParserTest {

    /**
     * Test evaluating the parser with multiple lines.
     */
    @Test
    public void testEvaluateMultipleLines() {
        ExpressionParser parser = new FixedPositionExpressionParser("test", 1, 0, 2);
        String text = "This is a test\r\non 2 lines.";
        assertEquals("on", parser.evaluate(text));
    }

    /**
     * Test evaluating the parser with no line number.
     */
    @Test
    public void testEvaluateNoLine() {
        ExpressionParser parser = new FixedPositionExpressionParser("test", null, 0, 2);
        String text = "This is a test\r\non 2 lines.";
        assertEquals("Th", parser.evaluate(text));
    }

    /**
     * Test evaluating the parser with no start position.
     */
    @Test
    public void testEvaluateNoStart() {
        ExpressionParser parser = new FixedPositionExpressionParser("test", 1, null, 2);
        String text = "This is a test\r\non 2 lines.";
        assertEquals("on", parser.evaluate(text));
    }


    /**
     * Test evaluating the parser with no end position.
     */
    @Test
    public void testEvaluateNoEnd() {
        ExpressionParser parser = new FixedPositionExpressionParser("test", 1, 3, null);
        String text = "This is a test\r\non 2 lines.";
        assertEquals("2 lines.", parser.evaluate(text));
    }


}
