package com.jecstar.etm.processor.processor;

import java.io.Closeable;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.EventCommand;
import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;
import com.lmax.disruptor.EventHandler;

public class PersistingEventHandler implements EventHandler<TelemetryEvent>, Closeable {

	private final long ordinal;
	private final long numberOfConsumers;
	private final Timer timer;
	
	private TelemetryEventRepository telemetryEventRepository;
	
	public PersistingEventHandler(final TelemetryEventRepository telemetryEventRepository, final long ordinal, final long numberOfConsumers, final Timer timer) {
		this.telemetryEventRepository = telemetryEventRepository;
	    this.ordinal = ordinal;
	    this.numberOfConsumers = numberOfConsumers;
	    this.timer = timer;
	}

	@Override
	public void onEvent(TelemetryEvent event, long sequence, boolean endOfBatch) throws Exception {
		if (event.ignore || EventCommand.NOOP.equals(event.eventCommand)) {
			return;
		}
		if (EventCommand.FLUSH_DOCUMENTS.equals(event.eventCommand)) {
			this.telemetryEventRepository.flushDocuments();
			return;
		}
		if (!EventCommand.PROCESS.equals(event.eventCommand) || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
		final Context timerContext = this.timer.time();
		try {
			this.telemetryEventRepository.persistTelemetryEvent(event);
			// Initialize the event to help the GC process reclaiming unused space. Otherwise the event will be kept on the ringbuffer untill it is overwritten with the new event. 
			event.initialize();
		} finally {
			timerContext.stop();
		}
	}

	@Override
	public void close() {
		this.telemetryEventRepository.flushDocuments();
	}

}
