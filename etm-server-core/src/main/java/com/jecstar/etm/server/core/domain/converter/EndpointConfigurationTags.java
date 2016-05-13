package com.jecstar.etm.server.core.domain.converter;

public interface EndpointConfigurationTags {

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
