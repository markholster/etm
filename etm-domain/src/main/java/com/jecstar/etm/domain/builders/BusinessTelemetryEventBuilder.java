package com.jecstar.etm.domain.builders;

import com.jecstar.etm.domain.BusinessTelemetryEvent;

public class BusinessTelemetryEventBuilder extends TelemetryEventBuilder<BusinessTelemetryEvent, BusinessTelemetryEventBuilder> {

	public BusinessTelemetryEventBuilder() {
		super(new BusinessTelemetryEvent());
	}
	
}
