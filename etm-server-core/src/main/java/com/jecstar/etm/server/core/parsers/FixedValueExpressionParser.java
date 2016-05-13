package com.jecstar.etm.server.core.parsers;

public class FixedValueExpressionParser implements ExpressionParser {

	private final String value;

	public FixedValueExpressionParser(final String value) {
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
