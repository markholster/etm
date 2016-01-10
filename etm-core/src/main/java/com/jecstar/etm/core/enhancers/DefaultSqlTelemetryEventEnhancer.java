package com.jecstar.etm.core.enhancers;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.jecstar.etm.core.domain.PayloadFormat;
import com.jecstar.etm.core.domain.SqlTelemetryEvent;

public class DefaultSqlTelemetryEventEnhancer implements SqlTelemetryEventEnhancer {

	@Override
	public void enhance(final SqlTelemetryEvent event, final ZonedDateTime enhanceTime) {
		if (event.id == null) {
			event.id = UUID.randomUUID().toString();
		}
		if (event.payloadFormat == null) {
			event.payloadFormat = PayloadFormat.SQL;
		}
		if (event.writingEndpointHandler.handlingTime == null) {
			event.writingEndpointHandler.handlingTime = enhanceTime;
		}
	}
}
