package com.jecstar.etm.core.enhancers;

import java.time.ZonedDateTime;

import com.jecstar.etm.domain.PayloadFormat;
import com.jecstar.etm.domain.SqlTelemetryEvent;

public class DefaultSqlTelemetryEventEnhancer extends AbstractDefaultTelemetryEventEnhancer implements SqlTelemetryEventEnhancer {

	@Override
	public String getName() {
		return "Default SQL Enhancer";
	}
	
	@Override
	public void enhance(final SqlTelemetryEvent event, final ZonedDateTime enhanceTime) {
		if (event.payloadFormat == null) {
			event.payloadFormat = PayloadFormat.SQL;
		}
		doEnhance(event, enhanceTime);
	}

}
