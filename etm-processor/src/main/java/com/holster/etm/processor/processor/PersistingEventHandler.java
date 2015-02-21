package com.holster.etm.processor.processor;

import com.holster.etm.core.configuration.EtmConfiguration;
import com.holster.etm.processor.TelemetryEvent;
import com.holster.etm.processor.repository.TelemetryEventRepository;
import com.lmax.disruptor.EventHandler;

public class PersistingEventHandler implements EventHandler<TelemetryEvent> {

	private final long ordinal;
	private final long numberOfConsumers;
	private final EtmConfiguration etmConfiguration;
	
	private TelemetryEventRepository telemetryEventRepository;
	
	
	
	public PersistingEventHandler(final TelemetryEventRepository telemetryEventRepository, final long ordinal, final long numberOfConsumers, final EtmConfiguration etmConfiguration) {
		this.telemetryEventRepository = telemetryEventRepository;
	    this.ordinal = ordinal;
	    this.numberOfConsumers = numberOfConsumers;
	    this.etmConfiguration = etmConfiguration;
	}

	@Override
	public void onEvent(TelemetryEvent event, long sequence, boolean endOfBatch) throws Exception {
		if (event.ignore || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
//		long start = System.nanoTime();
		this.telemetryEventRepository.persistTelemetryEvent(event, this.etmConfiguration.getStatisticsTimeUnit(), this.etmConfiguration.getDataRetentionCheckInterval());
//		Statistics.persistingTime.addAndGet(System.nanoTime() - start);
	}

}
