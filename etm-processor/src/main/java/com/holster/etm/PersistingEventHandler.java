package com.holster.etm;

import com.holster.etm.repository.TelemetryEventRepository;
import com.lmax.disruptor.EventHandler;

public class PersistingEventHandler implements EventHandler<TelemetryEvent> {

	private final long ordinal;
	private final long numberOfConsumers;
	
	private TelemetryEventRepository telemetryEventRepository;
	
	public PersistingEventHandler(final TelemetryEventRepository telemetryEventRepository, final long ordinal, final long numberOfConsumers) {
		this.telemetryEventRepository = telemetryEventRepository;
	    this.ordinal = ordinal;
	    this.numberOfConsumers = numberOfConsumers;
	}

	@Override
	public void onEvent(TelemetryEvent event, long sequence, boolean endOfBatch) throws Exception {
		if (event.ignore || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
		this.telemetryEventRepository.persistTelemetryEvent(event);
	}

}
