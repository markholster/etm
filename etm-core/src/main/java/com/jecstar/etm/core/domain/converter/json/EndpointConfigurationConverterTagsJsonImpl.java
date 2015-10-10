package com.jecstar.etm.core.domain.converter.json;

import com.jecstar.etm.core.domain.converter.EndpointConfigurationConverterTags;

public class EndpointConfigurationConverterTagsJsonImpl implements EndpointConfigurationConverterTags {

	@Override
	public String getReadingApplicationParsersTag() {
		return "reading_application_parsers";
	}

	@Override
	public String getWritingApplicationParsersTag() {
		return "writing_application_parsers";
	}

	@Override
	public String getEventNameParsersTag() {
		return "event_name_parsers";
	}

	@Override
	public String getCorrelationDataParsersTag() {
		return "correlation_data_parsers";
	}

	@Override
	public String getExtractionDataParsersTag() {
		return "extraction_data_parsers";
	}

	@Override
	public String getExpressionParserTypeTag() {
		return "type";
	}

	@Override
	public String getLineIndexTag() {
		return "line_ix";
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
	public String getPathTag() {
		return "path";
	}

	@Override
	public String getExpressionTag() {
		return "expression";
	}

	@Override
	public String getTemplateTag() {
		return "template";
	}

}
