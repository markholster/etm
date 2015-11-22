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
		if (event.readingEndpointHandler.isSet()) {
			if (added) {
				buffer.append(", ");
			}
			buffer.append("\"" + getTags().getReadingEndpointHandlerTag() + "\": ");
			added = addEndpointHandlerToJsonBuffer(event.readingEndpointHandler, buffer, true, getTags()) || added;
		}
		return added;
	}

	@Override
	void doConvert(HttpTelemetryEvent telemetryEvent, Map<String, Object> valueMap) {
		telemetryEvent.httpEventType =  HttpEventType.saveValueOf(getString(getTags().getHttpEventTypeTag(), valueMap));
		telemetryEvent.readingEndpointHandler.initialize(createEndpointFormValueMapHandler(getObject(getTags().getReadingEndpointHandlerTag(), valueMap)));
	}

}
