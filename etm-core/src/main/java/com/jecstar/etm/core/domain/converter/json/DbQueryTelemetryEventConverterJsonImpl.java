package com.jecstar.etm.core.domain.converter.json;

import java.util.Map;

import com.jecstar.etm.core.domain.DbQueryTelemetryEvent;
import com.jecstar.etm.core.domain.DbQueryTelemetryEvent.DbQueryEventType;

public class DbQueryTelemetryEventConverterJsonImpl extends AbstractJsonTelemetryEventConverter<DbQueryTelemetryEvent>{

	@Override
	boolean doConvert(DbQueryTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
		boolean added = !firstElement;
		if (event.dbQueryEventType != null) {
			added = addStringElementToJsonBuffer(getTags().getDbQueryEventTypeTag(), event.dbQueryEventType.name(), buffer, !added) || added;
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
	void doConvert(DbQueryTelemetryEvent telemetryEvent, Map<String, Object> valueMap) {
		telemetryEvent.dbQueryEventType =  DbQueryEventType.saveValueOf(getString(getTags().getDbQueryEventTypeTag(), valueMap));
		telemetryEvent.readingEndpointHandler.initialize(createEndpointFormValueMapHandler(getObject(getTags().getReadingEndpointHandlerTag(), valueMap)));
	}

}
