package com.jecstar.etm.domain.writers.json;

import com.jecstar.etm.domain.HttpTelemetryEvent;

public class HttpTelemetryEventWriterJsonImpl extends AbstractJsonTelemetryEventWriter<HttpTelemetryEvent>{

	@Override
	boolean doWrite(HttpTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
		boolean added = !firstElement;
		if (event.expiry != null) {
			added = this.jsonWriter.addLongElementToJsonBuffer(getTags().getExpiryTag(), event.expiry.toInstant().toEpochMilli(), buffer, !added) || added;
		}
		if (event.httpEventType != null) {
			added = this.jsonWriter.addStringElementToJsonBuffer(getTags().getHttpEventTypeTag(), event.httpEventType.name(), buffer, !added) || added;
		}
		return added;
	}

}
