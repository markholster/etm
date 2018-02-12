package com.jecstar.etm.domain;

/**
 * A <code>TelemetryEvent</code> that describes a business event that occurred in an application.
 */
public class BusinessTelemetryEvent extends TelemetryEvent<BusinessTelemetryEvent> {

    @Override
    public BusinessTelemetryEvent initialize() {
        super.internalInitialize();
        return this;
    }

    @Override
    public BusinessTelemetryEvent initialize(BusinessTelemetryEvent copy) {
        super.internalInitialize(copy);
        return this;
    }

}
