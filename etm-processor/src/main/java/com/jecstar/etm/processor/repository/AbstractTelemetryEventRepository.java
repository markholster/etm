package com.jecstar.etm.processor.repository;

import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.TelemetryMessageEvent;

public abstract class AbstractTelemetryEventRepository implements TelemetryEventRepository {

	@Override
    public final void persistTelemetryMessageEvent(TelemetryMessageEvent event) {
		startPersist(event);
		addTelemetryMessageEvent(event);
		endPersist();
    }
	

	protected abstract void startPersist(TelemetryEvent event);
	protected abstract void endPersist();
	protected abstract void addTelemetryMessageEvent(TelemetryMessageEvent event);
	

}
