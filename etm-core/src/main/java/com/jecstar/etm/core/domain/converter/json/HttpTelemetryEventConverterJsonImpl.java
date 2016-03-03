package com.jecstar.etm.core.domain.converter.json;

import java.util.Map;

import com.jecstar.etm.core.domain.HttpTelemetryEvent;
import com.jecstar.etm.core.domain.HttpTelemetryEvent.HttpEventType;

public class HttpTelemetryEventConverterJsonImpl extends AbstractJsonTelemetryEventConverter<HttpTelemetryEvent>{

	@Override
	boolean doConvert(HttpTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
		boolean added = !firstElement;
		if (event.httpEventType != null) {
			added = addStringElementToJsonBuffer(getTags().getHttpEventTypeTag(), event.httpEventType.name(), buffer, !added) || added;
		}
		return added;
	}

	@Override
	void doConvert(HttpTelemetryEvent telemetryEvent, Map<String, Object> valueMap) {
		telemetryEvent.httpEventType =  HttpEventType.safeValueOf(getString(getTags().getHttpEventTypeTag(), valueMap));
	}

}
