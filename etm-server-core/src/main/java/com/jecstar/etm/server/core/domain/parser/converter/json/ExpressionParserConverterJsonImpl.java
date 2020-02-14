/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.server.core.domain.parser.converter.json;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
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
            var expression = this.converter.getString(this.tags.getExpressionTag(), valueMap);
            var group = this.converter.getInteger(this.tags.getGroupTag(), valueMap, 1);
            var canonicalEquivalence = this.converter.getBoolean(this.tags.getCanonicalEquivalenceTag(), valueMap, false);
            var caseInsensitive = this.converter.getBoolean(this.tags.getCaseInsensitiveTag(), valueMap, false);
            var dotAll = this.converter.getBoolean(this.tags.getDotallTag(), valueMap, false);
            var literal = this.converter.getBoolean(this.tags.getLiteralTag(), valueMap, false);
            var multiLine = this.converter.getBoolean(this.tags.getMultilineTag(), valueMap, false);
            var unicodeCase = this.converter.getBoolean(this.tags.getUnicodeCaseTag(), valueMap, false);
            var unicodeCharacterClass = this.converter.getBoolean(this.tags.getUnicodeCharacterClassTag(), valueMap, false);
            var unixLines = this.converter.getBoolean(this.tags.getUnixLinesTag(), valueMap, false);
            return new RegexExpressionParser(name, expression, group,
                    canonicalEquivalence,
                    caseInsensitive,
                    dotAll,
                    literal,
                    multiLine,
                    unicodeCase,
                    unicodeCharacterClass,
                    unixLines);
        } else if ("javascript".equals(type)) {
            String script = this.converter.getString(this.tags.getExpressionTag(), valueMap);
            String mainFunction = this.converter.getString(this.tags.getMainFunctionTag(), valueMap);
            return new JavascriptExpressionParser(name, script, mainFunction);
        }
        if (log.isErrorLevelEnabled()) {
            log.logErrorMessage("Unknown expression parser type: '" + type + "'.");
        }
        throw new EtmException(EtmException.INVALID_EXPRESSION_PARSER_TYPE);
    }

    @Override
    public String write(ExpressionParser expressionParser, boolean withNamespace) {
        final var builder = new JsonBuilder();
        builder.startObject();
        if (withNamespace) {
            builder.field(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER);
            builder.startObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER);
        }
        builder.field(this.tags.getNameTag(), expressionParser.getName());
        if (expressionParser instanceof FixedPositionExpressionParser) {
            var parser = (FixedPositionExpressionParser) expressionParser;
            builder.field(this.tags.getTypeTag(), "fixed_position");
            builder.field(this.tags.getLineTag(), parser.getLine());
            builder.field(this.tags.getStartIndexTag(), parser.getStartIx());
            builder.field(this.tags.getEndIndexTag(), parser.getEndIx());
        } else if (expressionParser instanceof FixedValueExpressionParser) {
            var parser = (FixedValueExpressionParser) expressionParser;
            builder.field(this.tags.getTypeTag(), "fixed_value");
            builder.field(this.tags.getValueTag(), parser.getValue());
        } else if (expressionParser instanceof JsonPathExpressionParser) {
            var parser = (JsonPathExpressionParser) expressionParser;
            builder.field(this.tags.getTypeTag(), "jsonpath");
            builder.field(this.tags.getExpressionTag(), parser.getExpression());
        } else if (expressionParser instanceof XPathExpressionParser) {
            var parser = (XPathExpressionParser) expressionParser;
            builder.field(this.tags.getTypeTag(), "xpath");
            builder.field(this.tags.getExpressionTag(), parser.getExpression());
        } else if (expressionParser instanceof XsltExpressionParser) {
            var parser = (XsltExpressionParser) expressionParser;
            builder.field(this.tags.getTypeTag(), "xslt");
            builder.field(this.tags.getTemplateTag(), parser.getTemplate());
        } else if (expressionParser instanceof CopyValueExpressionParser) {
            builder.field(this.tags.getTypeTag(), "copy_value");
        } else if (expressionParser instanceof RegexExpressionParser) {
            var parser = (RegexExpressionParser) expressionParser;
            builder.field(this.tags.getTypeTag(), "regex");
            builder.field(this.tags.getExpressionTag(), parser.getExpression());
            builder.field(this.tags.getGroupTag(), parser.getGroup());
            builder.field(this.tags.getCanonicalEquivalenceTag(), parser.isCanonicalEquivalence());
            builder.field(this.tags.getCaseInsensitiveTag(), parser.isCaseInsensitive());
            builder.field(this.tags.getDotallTag(), parser.isDotall());
            builder.field(this.tags.getLiteralTag(), parser.isLiteral());
            builder.field(this.tags.getMultilineTag(), parser.isMultiline());
            builder.field(this.tags.getUnicodeCaseTag(), parser.isUnicodeCase());
            builder.field(this.tags.getUnicodeCharacterClassTag(), parser.isUnicodeCharacterClass());
            builder.field(this.tags.getUnixLinesTag(), parser.isUnixLines());
        } else if (expressionParser instanceof JavascriptExpressionParser) {
            var parser = (JavascriptExpressionParser) expressionParser;
            builder.field(this.tags.getTypeTag(), "javascript");
            builder.field(this.tags.getExpressionTag(), parser.getScript());
            builder.field(this.tags.getMainFunctionTag(), parser.getMainFunction());
        } else {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Unknown expression parser type: '" + expressionParser.getClass().getName() + "'.");
            }
            throw new EtmException(EtmException.INVALID_EXPRESSION_PARSER_TYPE);
        }
        if (withNamespace) {
            builder.endObject();
        }
        builder.endObject();
        return builder.build();
    }

    @Override
    public ExpressionParserTags getTags() {
        return this.tags;
    }

}
