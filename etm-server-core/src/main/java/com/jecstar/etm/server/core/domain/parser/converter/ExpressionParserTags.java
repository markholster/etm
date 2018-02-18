package com.jecstar.etm.server.core.domain.parser.converter;

public interface ExpressionParserTags {

    String getNameTag();

    String getTypeTag();

    String getLineTag();

    String getStartIndexTag();

    String getEndIndexTag();

    String getValueTag();

    String getExpressionTag();

    String getTemplateTag();

    String getGroupTag();

    String getCanonicalEquivalenceTag();

    String getCaseInsensitiveTag();

    String getDotallTag();

    String getLiteralTag();

    String getMultilineTag();

    String getUnicodeCaseTag();

    String getUnicodeCharacterClassTag();

    String getUnixLinesTag();
}
