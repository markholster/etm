package com.jecstar.etm.server.core.domain.converter.json;

import java.util.Map;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.converter.ExpressionParserConverter;
import com.jecstar.etm.server.core.domain.converter.ExpressionParserTags;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.parsers.ExpressionParser;
import com.jecstar.etm.server.core.parsers.FixedPositionExpressionParser;
import com.jecstar.etm.server.core.parsers.FixedValueExpressionParser;
import com.jecstar.etm.server.core.parsers.JsonPathExpressionParser;
import com.jecstar.etm.server.core.parsers.XPathExpressionParser;
import com.jecstar.etm.server.core.parsers.XsltExpressionParser;

public class ExpressionParserConverterJsonImpl implements ExpressionParserConverter<String> {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ExpressionParserConverterJsonImpl.class);
	
	private final ExpressionParserTags tags = new ExpressionParserTagsJsonImpl();
	private final JsonConverter converter = new JsonConverter();
	
	@Override
	public ExpressionParser read(String content) {
		Map<String, Object> valueMap = this.converter.toMap(content);
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
		boolean added = false;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getNameTag(), expressionParser.getName(), result, !added) || added;
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
		} else {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Unknown expression parser type: '" + expressionParser.getClass().getName() + "'.");
			}
			throw new EtmException(EtmException.INVALID_EXPRESSION_PARSER_TYPE);
		}
		result.append("}");
		return result.toString();
	}

	@Override
	public ExpressionParserTags getTags() {
		return this.tags;
	}

}
