package com.jecstar.etm.core.domain.converter.json;

import java.util.Map;

import com.jecstar.etm.core.domain.LogTelemetryEvent;

public class LogTelemetryEventConverterJsonImpl extends AbstractJsonTelemetryEventConverter<LogTelemetryEvent>{

	@Override
	boolean doConvert(LogTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
		boolean added = !firstElement;
		added = addStringElementToJsonBuffer(getTags().getLogLevelTag(), event.logLevel, buffer, !added) || added;
		added = addStringElementToJsonBuffer(getTags().getStackTraceTag(), event.stackTrace, buffer, !added) || added;
		return added;
	}

	@Override
	void doConvert(LogTelemetryEvent telemetryEvent, Map<String, Object> valueMap) {
		telemetryEvent.logLevel = getString(getTags().getLogLevelTag(), valueMap);
		telemetryEvent.stackTrace = getString(getTags().getStackTraceTag(), valueMap);
	}

}
