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

public class RegexExpressionParserTest {

    @Test
    public void testReplacement() {
        final String password = "ThisIsSuperSecret";
        final String changeText = "[REDACTED]]";
        final String payload = "username=SY0002101243424&password=" + password + "&scope=Schade.PartnerPortaal.Web&grant_type=password";
        RegexExpressionParser parser = new RegexExpressionParser("XPathExpressionParserTest"
                , "(.*password=|client_secret=)(.*?)(&.*)"
                , 2
                , false
                , true
                , false
                , false
                , false
                , false
                , false
                , false);

        String replacedPayload = parser.replace(payload, "$1" + changeText + "$3", false);
        assertEquals(payload.replace(password, changeText), replacedPayload);
    }
}
