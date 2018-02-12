package com.jecstar.etm.domain.builder;

import com.jecstar.etm.domain.BusinessTelemetryEvent;

public class BusinessTelemetryEventBuilder extends TelemetryEventBuilder<BusinessTelemetryEvent, BusinessTelemetryEventBuilder> {

    public BusinessTelemetryEventBuilder() {
        super(new BusinessTelemetryEvent());
    }

}
