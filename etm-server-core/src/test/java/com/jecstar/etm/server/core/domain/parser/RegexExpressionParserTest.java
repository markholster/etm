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
