package com.jecstar.etm.server.core.domain.converter;

import com.jecstar.etm.server.core.parsers.ExpressionParser;

public interface ExpressionParserConverter<T> {

	ExpressionParser read(T content);
	
	T write(ExpressionParser expressionParser);
	
	ExpressionParserTags getTags();
}
