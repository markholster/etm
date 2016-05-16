package com.jecstar.etm.domain.writers.json;

import com.jecstar.etm.domain.BusinessTelemetryEvent;

public class BusinessTelemetryEventWriterJsonImpl extends AbstractJsonTelemetryEventWriter<BusinessTelemetryEvent>{

	@Override
	boolean doWrite(BusinessTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
		return firstElement;
	}

}
