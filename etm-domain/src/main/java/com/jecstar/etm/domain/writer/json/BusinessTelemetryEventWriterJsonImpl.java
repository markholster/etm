package com.jecstar.etm.domain.writer.json;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.writer.TelemetryEventTags;

public class BusinessTelemetryEventWriterJsonImpl extends AbstractJsonTelemetryEventWriter<BusinessTelemetryEvent>{

	@Override
	String getType() {
		return TelemetryEventTags.EVENT_TYPE_BUSINESS;
	}

	@Override
	boolean doWrite(BusinessTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
		return firstElement;
	}

}
