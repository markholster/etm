package com.jecstar.etm.server.core.parsers;

public interface ExpressionParser {
	
	String getName();

	String evaluate(String content);
}
