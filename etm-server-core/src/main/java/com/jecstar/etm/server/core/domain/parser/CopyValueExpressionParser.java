package com.jecstar.etm.server.core.domain.parser;

public class CopyValueExpressionParser extends AbstractExpressionParser {

    public CopyValueExpressionParser(String name) {
        super(name);
    }

    @Override
    public String evaluate(String content) {
        return content;
    }
}
