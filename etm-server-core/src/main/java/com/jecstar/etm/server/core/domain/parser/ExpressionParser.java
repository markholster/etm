package com.jecstar.etm.server.core.domain.parser;

public interface ExpressionParser {

    String getName();

    String evaluate(String content);
}
