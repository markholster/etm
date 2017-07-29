package com.jecstar.etm.server.core.parsers;

abstract class AbstractExpressionParser implements ExpressionParser {

	private final String name;

	AbstractExpressionParser(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return this.name;
	}
}
