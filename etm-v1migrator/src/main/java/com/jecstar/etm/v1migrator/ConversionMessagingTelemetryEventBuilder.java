package com.jecstar.etm.v1migrator;

import java.util.Collection;

import com.jecstar.etm.domain.builder.MessagingTelemetryEventBuilder;

public class ConversionMessagingTelemetryEventBuilder extends MessagingTelemetryEventBuilder {


	private Collection<String> correlations;

	public ConversionMessagingTelemetryEventBuilder setCorrelations(Collection<String> correlations) {
		this.correlations = correlations;
		return this;
	}
	
	public Collection<String> getCorrelations() {
		return correlations;
	}
}
