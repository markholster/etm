package com.jecstar.etm.core.domain.converter.json;

import java.util.Map;

import com.jecstar.etm.domain.SqlTelemetryEvent;
import com.jecstar.etm.domain.SqlTelemetryEvent.SqlEventType;

public class SqlTelemetryEventConverterJsonImpl extends AbstractJsonTelemetryEventConverter<SqlTelemetryEvent>{

	@Override
	boolean doConvert(SqlTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
		boolean added = !firstElement;
		if (event.sqlEventType != null) {
			added = addStringElementToJsonBuffer(getTags().getSqlEventTypeTag(), event.sqlEventType.name(), buffer, !added) || added;
		}
		return added;
	}

	@Override
	void doConvert(SqlTelemetryEvent telemetryEvent, Map<String, Object> valueMap) {
		telemetryEvent.sqlEventType =  SqlEventType.safeValueOf(getString(getTags().getSqlEventTypeTag(), valueMap));
	}

}
