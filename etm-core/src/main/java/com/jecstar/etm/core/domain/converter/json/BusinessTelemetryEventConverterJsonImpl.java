package com.jecstar.etm.core.domain.converter.json;

import java.util.Map;

import com.jecstar.etm.core.domain.BusinessTelemetryEvent;

public class BusinessTelemetryEventConverterJsonImpl extends AbstractJsonTelemetryEventConverter<BusinessTelemetryEvent>{

	@Override
	boolean doConvert(BusinessTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
		return firstElement;
	}

	@Override
	void doConvert(BusinessTelemetryEvent telemetryEvent, Map<String, Object> valueMap) {
	}

}
