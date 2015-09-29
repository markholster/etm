package com.jecstar.etm.processor.processor;

import java.io.Closeable;
import java.io.IOException;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.domain.TelemetryEvent;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;
import com.lmax.disruptor.EventHandler;

public class PersistingEventHandler implements EventHandler<TelemetryCommand>, Closeable {

	private final long ordinal;
	private final long numberOfConsumers;
	private final Timer timer;

	private TelemetryEventRepository telemetryEventRepository;

	public PersistingEventHandler(final TelemetryEventRepository telemetryEventRepository, final long ordinal,
	        final long numberOfConsumers, final MetricRegistry metricRegistry) {
		this.telemetryEventRepository = telemetryEventRepository;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		this.timer = metricRegistry.timer("event-processor-persisting");
	}

	@Override
	public void onEvent(TelemetryCommand command, long sequence, boolean endOfBatch) throws Exception {
		if (sequence % this.numberOfConsumers != this.ordinal) {
			return;
		}
		switch (command.commandType) {
		case EVENT:
			persistTelemetryMessageEvent(command.event);
			break;
		default:
			break;
		}
	}

	private void persistTelemetryMessageEvent(TelemetryEvent event) {
		final Context timerContext = this.timer.time();
		try {
			this.telemetryEventRepository.persistTelemetryEvent(event);
		} finally {
			timerContext.stop();
		}

	}

	@Override
    public void close() throws IOException {
		this.telemetryEventRepository.close();
    }

}
