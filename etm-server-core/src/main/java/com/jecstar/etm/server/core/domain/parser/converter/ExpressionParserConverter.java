package com.jecstar.etm.server.core.domain.parser.converter;

import com.jecstar.etm.server.core.domain.parser.ExpressionParser;

public interface ExpressionParserConverter<T> {

    ExpressionParser read(T content);

    T write(ExpressionParser expressionParser);

    ExpressionParserTags getTags();
}
