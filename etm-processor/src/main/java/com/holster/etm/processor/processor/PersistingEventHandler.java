package com.holster.etm.processor.processor;

import java.util.concurrent.TimeUnit;

import com.holster.etm.processor.TelemetryEvent;
import com.holster.etm.processor.repository.TelemetryEventRepository;
import com.lmax.disruptor.EventHandler;

public class PersistingEventHandler implements EventHandler<TelemetryEvent> {

	private final long ordinal;
	private final long numberOfConsumers;
	private final TimeUnit statisticsTimeUnit;
	
	private TelemetryEventRepository telemetryEventRepository;
	
	
	public PersistingEventHandler(final TelemetryEventRepository telemetryEventRepository, final long ordinal, final long numberOfConsumers, final TimeUnit statisticsTimeUnit) {
		this.telemetryEventRepository = telemetryEventRepository;
	    this.ordinal = ordinal;
	    this.numberOfConsumers = numberOfConsumers;
	    this.statisticsTimeUnit = statisticsTimeUnit;
	}

	@Override
	public void onEvent(TelemetryEvent event, long sequence, boolean endOfBatch) throws Exception {
		if (event.ignore || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
//		long start = System.nanoTime();
		this.telemetryEventRepository.persistTelemetryEvent(event, this.statisticsTimeUnit);
//		Statistics.persistingTime.addAndGet(System.nanoTime() - start);
	}

}
