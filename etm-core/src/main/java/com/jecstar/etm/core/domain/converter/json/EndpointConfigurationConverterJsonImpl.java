package com.jecstar.etm.core.domain.converter.json;

import java.util.List;
import java.util.Map;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.domain.EndpointConfiguration;
import com.jecstar.etm.core.domain.converter.EndpointConfigurationConverter;
import com.jecstar.etm.core.domain.converter.EndpointConfigurationConverterTags;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.core.parsers.FixedPositionExpressionParser;
import com.jecstar.etm.core.parsers.FixedValueExpressionParser;
import com.jecstar.etm.core.parsers.JsonPathExpressionParser;
import com.jecstar.etm.core.parsers.XPathExpressionParser;
import com.jecstar.etm.core.parsers.XsltExpressionParser;

import net.sf.saxon.Configuration;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.xpath.XPathFactoryImpl;

public class EndpointConfigurationConverterJsonImpl extends AbstractJsonConverter implements EndpointConfigurationConverter<String> {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(EndpointConfigurationConverterJsonImpl.class);


	private static final String EXPR_TYPE_FIXED_POSITION = "fixed_position"; 
	private static final String EXPR_TYPE_FIXED_VALUE = "fixed_value"; 
	private static final String EXPR_TYPE_JSON_PATH = "json_path"; 
	private static final String EXPR_TYPE_XPATH = "xpath"; 
	private static final String EXPR_TYPE_XSLT = "xlst";
	private static final XPath XPATH;
	private static final TransformerFactory TRANSFORMER_FACTORY;
	
	static {
	    Configuration config = new Configuration();
	    config.setErrorListener(new ErrorListener() {
			@Override
			public void warning(TransformerException exception) throws TransformerException {
				log.logWarningMessage(exception.getMessage(), exception);
			}
			
			@Override
			public void fatalError(TransformerException exception) throws TransformerException {
				log.logFatalMessage(exception.getMessage(), exception);
			}
			
			@Override
			public void error(TransformerException exception) throws TransformerException {
				log.logErrorMessage(exception.getMessage(), exception);
			}
		});
		XPathFactory xpf = new XPathFactoryImpl(config);
		XPATH = xpf.newXPath();
		TRANSFORMER_FACTORY = new TransformerFactoryImpl();

	}
			
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
			return new XPathExpressionParser(XPATH, expression);
		} else if (EXPR_TYPE_XSLT.equals(type)) {
			String template = getString(this.tags.getTemplateTag(), valueMap);
			return new XsltExpressionParser(TRANSFORMER_FACTORY, template);
		} else {
			throw new EtmException(EtmException.INVALID_EXPRESSION_PARSER_TYPE);
		}
	}
}
