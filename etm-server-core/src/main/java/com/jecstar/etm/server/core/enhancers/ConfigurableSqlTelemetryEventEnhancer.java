package com.jecstar.etm.server.core.enhancers;

import java.time.ZonedDateTime;

import com.jecstar.etm.domain.SqlTelemetryEvent;

public class ConfigurableSqlTelemetryEventEnhancer extends AbstractConfigurableTelemetryEventEnhancer implements SqlTelemetryEventEnhancer {

	@Override
	public String getName() {
		return "Configurable Sql Enhancer";
	}
	
	@Override
	public void enhance(final SqlTelemetryEvent event, final ZonedDateTime enhanceTime) {
		doEnhance(event, enhanceTime);
	}
	
}
