package com.jecstar.etm.server.core.enhancers;

import com.jecstar.etm.server.core.domain.parser.ExpressionParser;

public class DefaultTransformation {

    private ExpressionParser expressionParser;
    private String replacement;
    private boolean replaceAll;

    public ExpressionParser getExpressionParser() {
        return this.expressionParser;
    }

    public void setExpressionParser(ExpressionParser expressionParser) {
        this.expressionParser = expressionParser;
    }

    public String getReplacement() {
        return this.replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    public boolean isReplaceAll() {
        return this.replaceAll;
    }

    public void setReplaceAll(boolean replaceAll) {
        this.replaceAll = replaceAll;
    }
}
