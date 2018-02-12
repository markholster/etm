package com.jecstar.etm.server.core.domain.parser;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

public class JsonPathExpressionParser extends AbstractExpressionParser {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(JsonPathExpressionParser.class);

    private final JsonPath expression;

    static {
        Configuration.setDefaults(new JsonPathDefaults());
    }


    public JsonPathExpressionParser(final String name, final String expression) {
        super(name);
        this.expression = JsonPath.compile(expression);
        if (!this.expression.isDefinite()) {
            throw new EtmException(EtmException.INVALID_JSON_EXPRESSION);
        }
    }

    @Override
    public String evaluate(String content) {
        if (this.expression == null) {
            return null;
        }
        try {
            return JsonPath.parse(content).read(this.expression, String.class);
        } catch (Exception e) {
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Json expression '" + this.expression.getPath() + "' could not be evaluated against content '" + content + "'.", e);
            }
            return null;
        }
    }

    public String getExpression() {
        return this.expression.getPath();
    }

}
