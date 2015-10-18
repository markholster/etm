package com.jecstar.etm.core.domain.converter;

public interface EndpointConfigurationConverterTags {

	String getReadingApplicationParsersTag();
	String getWritingApplicationParsersTag();
	String getEventNameParsersTag();
	String getCorrelationDataParsersTag();
	String getExtractionDataParsersTag();
	
	String getExpressionParserTypeTag();
	String getLineIndexTag();
	String getStartIndexTag();
	String getEndIndexTag();
	String getValueTag();
	String getPathTag();
	String getExpressionTag();
	String getTemplateTag();
	
	String getCorrelationDataParserNameTag();
	String getCorrelationDataParserExpressionTag();
	
	String getExtractionDataParserNameTag();
	String getExtractionDataParserExpressionTag();

}
