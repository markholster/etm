package com.jecstar.etm.domain.writer.json;

import com.jecstar.etm.domain.LogTelemetryEvent;

public class LogTelemetryEventWriterJsonImpl extends AbstractJsonTelemetryEventWriter<LogTelemetryEvent>{

	@Override
	boolean doWrite(LogTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
		boolean added = !firstElement;
		added = this.jsonWriter.addStringElementToJsonBuffer(getTags().getLogLevelTag(), event.logLevel, buffer, !added) || added;
		added = this.jsonWriter.addStringElementToJsonBuffer(getTags().getStackTraceTag(), event.stackTrace, buffer, !added) || added;
		return added;
	}

}
