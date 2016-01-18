package com.jecstar.etm.core.domain;

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
