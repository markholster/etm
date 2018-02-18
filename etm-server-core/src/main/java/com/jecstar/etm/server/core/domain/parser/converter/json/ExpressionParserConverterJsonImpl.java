package com.jecstar.etm.server.core.domain.parser.converter.json;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.parser.*;
import com.jecstar.etm.server.core.domain.parser.converter.ExpressionParserConverter;
import com.jecstar.etm.server.core.domain.parser.converter.ExpressionParserTags;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.util.Map;

public class ExpressionParserConverterJsonImpl implements ExpressionParserConverter<String> {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(ExpressionParserConverterJsonImpl.class);

    private final ExpressionParserTags tags = new ExpressionParserTagsJsonImpl();
    private final JsonConverter converter = new JsonConverter();

    @Override
    public ExpressionParser read(String content) {
        return read(this.converter.toMap(content));
    }

    public ExpressionParser read(Map<String, Object> valueMap) {
        valueMap = this.converter.getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER, valueMap);
        String name = this.converter.getString(this.tags.getNameTag(), valueMap);
        String type = this.converter.getString(this.tags.getTypeTag(), valueMap);
        if ("fixed_position".equals(type)) {
            int line = this.converter.getInteger(this.tags.getLineTag(), valueMap, 0);
            int startIx = this.converter.getInteger(this.tags.getStartIndexTag(), valueMap, 0);
            int endIx = this.converter.getInteger(this.tags.getEndIndexTag(), valueMap, 1);
            if (line < 0) {
                if (log.isWarningLevelEnabled()) {
                    log.logWarningMessage("ExpressionParser '" + name + "' has a line number of " + line + ". Setting it to 0.");
                }
                line = 0;
            }
            if (startIx < 0) {
                if (log.isWarningLevelEnabled()) {
                    log.logWarningMessage("ExpressionParser '" + name + "' has a start index of " + startIx + ". Setting it to 0.");
                }
                startIx = 0;
            }
            if (endIx <= startIx) {
                if (log.isWarningLevelEnabled()) {
                    log.logWarningMessage("ExpressionParser '" + name + "' has an end index of " + endIx + " which is smaller than the start index " + startIx + ". Setting it to " + (startIx + 1) + ".");
                }
                endIx = startIx + 1;
            }
            return new FixedPositionExpressionParser(name, line, startIx, endIx);
        } else if ("fixed_value".equals(type)) {
            return new FixedValueExpressionParser(name, this.converter.getString(this.tags.getValueTag(), valueMap));
        } else if ("jsonpath".equals(type)) {
            return new JsonPathExpressionParser(name, this.converter.getString(this.tags.getExpressionTag(), valueMap));
        } else if ("xpath".equals(type)) {
            return new XPathExpressionParser(name, this.converter.getString(this.tags.getExpressionTag(), valueMap));
        } else if ("xslt".equals(type)) {
            return new XsltExpressionParser(name, this.converter.getString(this.tags.getTemplateTag(), valueMap));
        } else if ("copy_value".equals(type)) {
            return new CopyValueExpressionParser(name);
        } else if ("regex".equals(type)) {
            String expression = this.converter.getString(this.tags.getExpressionTag(), valueMap);
            int group = this.converter.getInteger(this.tags.getGroupTag(), valueMap, 1);
            boolean canonicalEquivalence = this.converter.getBoolean(this.tags.getCanonicalEquivalenceTag(), valueMap, false);
            boolean caseInsensitive = this.converter.getBoolean(this.tags.getCaseInsensitiveTag(), valueMap, false);
            boolean dotAll = this.converter.getBoolean(this.tags.getDotallTag(), valueMap, false);
            boolean literal = this.converter.getBoolean(this.tags.getLiteralTag(), valueMap, false);
            boolean multiLine = this.converter.getBoolean(this.tags.getMultilineTag(), valueMap, false);
            boolean unicodeCase = this.converter.getBoolean(this.tags.getUnicodeCaseTag(), valueMap, false);
            boolean unicodeCharacterClass = this.converter.getBoolean(this.tags.getUnicodeCharacterClassTag(), valueMap, false);
            boolean unixLines = this.converter.getBoolean(this.tags.getUnixLinesTag(), valueMap, false);
            return new RegexExpressionParser(name, expression, group,
                    canonicalEquivalence,
                    caseInsensitive,
                    dotAll,
                    literal,
                    multiLine,
                    unicodeCase,
                    unicodeCharacterClass,
                    unixLines);
        }
        if (log.isErrorLevelEnabled()) {
            log.logErrorMessage("Unknown expression parser type: '" + type + "'.");
        }
        throw new EtmException(EtmException.INVALID_EXPRESSION_PARSER_TYPE);
    }

    @Override
    public String write(ExpressionParser expressionParser) {
        StringBuilder result = new StringBuilder();
        result.append("{");
        this.converter.addStringElementToJsonBuffer(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER, result, true);
        result.append(", " + this.converter.escapeToJson(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER, true) + ": {");
        boolean added = this.converter.addStringElementToJsonBuffer(this.tags.getNameTag(), expressionParser.getName(), result, true);
        if (expressionParser instanceof FixedPositionExpressionParser) {
            FixedPositionExpressionParser parser = (FixedPositionExpressionParser) expressionParser;
            added = this.converter.addStringElementToJsonBuffer(this.tags.getTypeTag(), "fixed_position", result, !added) || added;
            added = this.converter.addIntegerElementToJsonBuffer(this.tags.getLineTag(), parser.getLine(), result, !added) || added;
            added = this.converter.addIntegerElementToJsonBuffer(this.tags.getStartIndexTag(), parser.getStartIx(), result, !added) || added;
            added = this.converter.addIntegerElementToJsonBuffer(this.tags.getEndIndexTag(), parser.getEndIx(), result, !added) || added;
        } else if (expressionParser instanceof FixedValueExpressionParser) {
            FixedValueExpressionParser parser = (FixedValueExpressionParser) expressionParser;
            added = this.converter.addStringElementToJsonBuffer(this.tags.getTypeTag(), "fixed_value", result, !added) || added;
            added = this.converter.addStringElementToJsonBuffer(this.tags.getValueTag(), parser.getValue(), result, !added) || added;
        } else if (expressionParser instanceof JsonPathExpressionParser) {
            JsonPathExpressionParser parser = (JsonPathExpressionParser) expressionParser;
            added = this.converter.addStringElementToJsonBuffer(this.tags.getTypeTag(), "jsonpath", result, !added) || added;
            added = this.converter.addStringElementToJsonBuffer(this.tags.getExpressionTag(), parser.getExpression(), result, !added) || added;
        } else if (expressionParser instanceof XPathExpressionParser) {
            XPathExpressionParser parser = (XPathExpressionParser) expressionParser;
            added = this.converter.addStringElementToJsonBuffer(this.tags.getTypeTag(), "xpath", result, !added) || added;
            added = this.converter.addStringElementToJsonBuffer(this.tags.getExpressionTag(), parser.getExpression(), result, !added) || added;
        } else if (expressionParser instanceof XsltExpressionParser) {
            XsltExpressionParser parser = (XsltExpressionParser) expressionParser;
            added = this.converter.addStringElementToJsonBuffer(this.tags.getTypeTag(), "xslt", result, !added) || added;
            added = this.converter.addStringElementToJsonBuffer(this.tags.getTemplateTag(), parser.getTemplate(), result, !added) || added;
        } else if (expressionParser instanceof CopyValueExpressionParser) {
            added = this.converter.addStringElementToJsonBuffer(this.tags.getTypeTag(), "copy_value", result, !added) || added;
        } else if (expressionParser instanceof RegexExpressionParser) {
            RegexExpressionParser parser = (RegexExpressionParser) expressionParser;
            added = this.converter.addStringElementToJsonBuffer(this.tags.getTypeTag(), "regex", result, !added) || added;
            added = this.converter.addStringElementToJsonBuffer(this.tags.getExpressionTag(), parser.getExpression(), result, !added) || added;
            added = this.converter.addIntegerElementToJsonBuffer(this.tags.getGroupTag(), parser.getGroup(), result, !added) || added;
            added = this.converter.addBooleanElementToJsonBuffer(this.tags.getCanonicalEquivalenceTag(), parser.isCanonicalEquivalence(), result, !added) || added;
            added = this.converter.addBooleanElementToJsonBuffer(this.tags.getCaseInsensitiveTag(), parser.isCaseInsensitive(), result, !added) || added;
            added = this.converter.addBooleanElementToJsonBuffer(this.tags.getDotallTag(), parser.isDotall(), result, !added) || added;
            added = this.converter.addBooleanElementToJsonBuffer(this.tags.getLiteralTag(), parser.isLiteral(), result, !added) || added;
            added = this.converter.addBooleanElementToJsonBuffer(this.tags.getMultilineTag(), parser.isMultiline(), result, !added) || added;
            added = this.converter.addBooleanElementToJsonBuffer(this.tags.getUnicodeCaseTag(), parser.isUnicodeCase(), result, !added) || added;
            added = this.converter.addBooleanElementToJsonBuffer(this.tags.getUnicodeCharacterClassTag(), parser.isUnicodeCharacterClass(), result, !added) || added;
            added = this.converter.addBooleanElementToJsonBuffer(this.tags.getUnixLinesTag(), parser.isUnixLines(), result, !added) || added;
        } else {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Unknown expression parser type: '" + expressionParser.getClass().getName() + "'.");
            }
            throw new EtmException(EtmException.INVALID_EXPRESSION_PARSER_TYPE);
        }
        result.append("}}");
        return result.toString();
    }

    @Override
    public ExpressionParserTags getTags() {
        return this.tags;
    }

}
