package com.jecstar.etm.server.core.domain.parser.converter.json;

import com.jecstar.etm.server.core.domain.parser.converter.ExpressionParserTags;

public class ExpressionParserTagsJsonImpl implements ExpressionParserTags {

    @Override
    public String getNameTag() {
        return "name";
    }

    @Override
    public String getTypeTag() {
        return "type";
    }

    @Override
    public String getLineTag() {
        return "line";
    }

    @Override
    public String getStartIndexTag() {
        return "start_ix";
    }

    @Override
    public String getEndIndexTag() {
        return "end_ix";
    }

    @Override
    public String getValueTag() {
        return "value";
    }

    @Override
    public String getExpressionTag() {
        return "expression";
    }

    @Override
    public String getTemplateTag() {
        return "template";
    }

    @Override
    public String getGroupTag() {
        return "group";
    }

    @Override
    public String getCanonicalEquivalenceTag() {
        return "canonical_equivalence";
    }

    @Override
    public String getCaseInsensitiveTag() {
        return "case_insensitive";
    }

    @Override
    public String getDotallTag() {
        return "dotall";
    }

    @Override
    public String getLiteralTag() {
        return "literal";
    }

    @Override
    public String getMultilineTag() {
        return "multiline";
    }

    @Override
    public String getUnicodeCaseTag() {
        return "unicode_case";
    }

    @Override
    public String getUnicodeCharacterClassTag() {
        return "unicode_character_class";
    }

    @Override
    public String getUnixLinesTag() {
        return "unix_lines";
    }
}
