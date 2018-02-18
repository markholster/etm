package com.jecstar.etm.server.core.domain.parser;

abstract class AbstractExpressionParser implements ExpressionParser {

    private final String name;

    AbstractExpressionParser(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isCapableOfReplacing() {
        return false;
    }

    @Override
    public String replace(String content, String replacement, boolean allValues) {
        throw new UnsupportedOperationException();
    }
}
