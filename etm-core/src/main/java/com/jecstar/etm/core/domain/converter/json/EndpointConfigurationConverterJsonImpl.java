package com.jecstar.etm.core.domain.converter.json;

import java.util.List;
import java.util.Map;

import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.domain.EndpointConfiguration;
import com.jecstar.etm.core.domain.converter.EndpointConfigurationConverter;
import com.jecstar.etm.core.domain.converter.EndpointConfigurationConverterTags;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.core.parsers.FixedPositionExpressionParser;
import com.jecstar.etm.core.parsers.FixedValueExpressionParser;
import com.jecstar.etm.core.parsers.JsonPathExpressionParser;
import com.jecstar.etm.core.parsers.XPathExpressionParser;
import com.jecstar.etm.core.parsers.XsltExpressionParser;

public class EndpointConfigurationConverterJsonImpl extends AbstractJsonConverter implements EndpointConfigurationConverter<String> {
	

	private static final String EXPR_TYPE_FIXED_POSITION = "fixed_position"; 
	private static final String EXPR_TYPE_FIXED_VALUE = "fixed_value"; 
	private static final String EXPR_TYPE_JSON_PATH = "json_path"; 
	private static final String EXPR_TYPE_XPATH = "xpath"; 
	private static final String EXPR_TYPE_XSLT = "xlst";
			
	private final EndpointConfigurationConverterTags tags = new EndpointConfigurationConverterTagsJsonImpl();

	@Override
	public EndpointConfiguration convert(String jsonContent) {
		Map<String, Object> valueMap = toMap(jsonContent);
		EndpointConfiguration config = new EndpointConfiguration();
		List<Map<String, Object>> readingApplicationMaps = getArray(this.tags.getReadingApplicationParsersTag(), valueMap);
		readingApplicationMaps.forEach(c -> config.readingApplicationParsers.add(convertToExpressionParser(c)));
		List<Map<String, Object>> writingApplicationMaps = getArray(this.tags.getWritingApplicationParsersTag(), valueMap);
		writingApplicationMaps.forEach(c -> config.writingApplicationParsers.add(convertToExpressionParser(c)));
		List<Map<String, Object>> eventNameMaps = getArray(this.tags.getEventNameParsersTag(), valueMap);
		eventNameMaps.forEach(c -> config.eventNameParsers.add(convertToExpressionParser(c)));
		List<Map<String, Object>> correlationDataParsers = getArray(this.tags.getCorrelationDataParsersTag(), valueMap);
		correlationDataParsers.forEach(c -> {
			String dataName = getString(this.tags.getCorrelationDataParserNameTag(), c);
			ExpressionParser expressionParser = convertToExpressionParser(getObject(this.tags.getCorrelationDataParserExpressionTag(), c));
			config.correlationDataParsers.put(dataName, expressionParser);
		});
		List<Map<String, Object>> extractionDataParsers = getArray(this.tags.getExtractionDataParsersTag(), valueMap);
		extractionDataParsers.forEach(c -> {
			String extractionName = getString(this.tags.getExtractionDataParserNameTag(), c);
			ExpressionParser expressionParser = convertToExpressionParser(getObject(this.tags.getExtractionDataParserExpressionTag(), c));
			config.extractionDataParsers.put(extractionName, expressionParser);
		});
		return config;
	}

	private ExpressionParser convertToExpressionParser(Map<String, Object> valueMap) {
		String type = getString(this.tags.getExpressionParserTypeTag(), valueMap);
		if (EXPR_TYPE_FIXED_POSITION.equals(type)) {
			Integer lineIx = getInteger(this.tags.getLineIndexTag(), valueMap);
			Integer startIx = getInteger(this.tags.getStartIndexTag(), valueMap);
			Integer endIx = getInteger(this.tags.getEndIndexTag(), valueMap);
			return new FixedPositionExpressionParser(lineIx, startIx, endIx);
		} else if (EXPR_TYPE_FIXED_VALUE.equals(type)) {
			String value = getString(this.tags.getValueTag(), valueMap);
			return new FixedValueExpressionParser(value);
		} else if (EXPR_TYPE_JSON_PATH.equals(type)) {
			String path = getString(this.tags.getPathTag(), valueMap);
			return new JsonPathExpressionParser(path);
		} else if (EXPR_TYPE_XPATH.equals(type)) {
			String expression = getString(this.tags.getExpressionTag(), valueMap);
			return new XPathExpressionParser(expression);
		} else if (EXPR_TYPE_XSLT.equals(type)) {
			String template = getString(this.tags.getTemplateTag(), valueMap);
			return new XsltExpressionParser(template);
		} else {
			throw new EtmException(EtmException.INVALID_EXPRESSION_PARSER_TYPE);
		}
	}

	@Override
	public EndpointConfigurationConverterTags getTags() {
		return this.tags;
	}
}
