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

import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class JavascriptExpressionParserTest {

    @Test
    public void testMultiThreading() throws InterruptedException {
        final var valueUppercase = "JECSTAR INNOVATION";
        final var valueLower = valueUppercase.toLowerCase();

        var javascriptExpressionParser = new JavascriptExpressionParser("test",
                "function main(content, replacement) {\n" +
                        "  return content.toLowerCase();\n" +
                        "}",
                "main"
        );

        var executorService = Executors.newFixedThreadPool(10);
        var futures = new HashSet<Future<?>>();
        for (var i = 0; i < 100000; i++) {
            var submit = executorService.submit(() -> {
                String value = javascriptExpressionParser.evaluate(valueUppercase);
                assertEquals(valueLower, value);
            });
            futures.add(submit);
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
        for (var future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                fail(e);
            }
        }
    }
}
