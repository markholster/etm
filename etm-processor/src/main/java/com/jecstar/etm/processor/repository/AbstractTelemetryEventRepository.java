package com.jecstar.etm.processor.repository;

import com.jecstar.etm.core.domain.TelemetryEvent;

public abstract class AbstractTelemetryEventRepository implements TelemetryEventRepository {

	@Override
    public final void persistTelemetryEvent(TelemetryEvent event) {
		startPersist(event);
		addTelemetryEvent(event);
		endPersist();
    }
	

	protected abstract void startPersist(TelemetryEvent event);
	protected abstract void endPersist();
	protected abstract void addTelemetryEvent(TelemetryEvent event);
	

}
