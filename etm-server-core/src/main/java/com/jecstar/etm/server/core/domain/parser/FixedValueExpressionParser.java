package com.jecstar.etm.server.core.domain.parser;

public class FixedValueExpressionParser extends AbstractExpressionParser {

    private final String value;

    public FixedValueExpressionParser(final String name, final String value) {
        super(name);
        this.value = value;
    }

    @Override
    public String evaluate(String content) {
        return this.value;
    }

    public String getValue() {
        return this.value;
    }

}
