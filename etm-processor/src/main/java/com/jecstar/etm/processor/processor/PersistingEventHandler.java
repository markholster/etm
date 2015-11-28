package com.jecstar.etm.processor.processor;

import java.io.Closeable;
import java.io.IOException;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.domain.converter.json.MessagingTelemetryEventConverterJsonImpl;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.processor.persisting.MessagingTelemetryEventPersister;
import com.lmax.disruptor.EventHandler;

public class PersistingEventHandler implements EventHandler<TelemetryCommand>, Closeable {

	private final long ordinal;
	private final long numberOfConsumers;
	private final Timer timer;
	private final CommandResources commandResources;
	private final MessagingTelemetryEventConverterJsonImpl messagingTelemetryEventConverter = new MessagingTelemetryEventConverterJsonImpl();

	public PersistingEventHandler(final long ordinal, final long numberOfConsumers, final CommandResources commandResources, final MetricRegistry metricRegistry) {
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		this.commandResources = commandResources;
		this.timer = metricRegistry.timer("event-processor-persisting");
	}

	@Override
	public void onEvent(TelemetryCommand command, long sequence, boolean endOfBatch) throws Exception {
		if (sequence % this.numberOfConsumers != this.ordinal) {
			return;
		}
		switch (command.commandType) {
		case MESSAGING_EVENT:
			final MessagingTelemetryEventPersister persister = this.commandResources.getPersister(command.commandType);
			final Context timerContext = this.timer.time();
			try {
				persister.persist(command.messagingTelemetryEvent, this.messagingTelemetryEventConverter);
			} finally {
				timerContext.stop();
			}
			break;
		default:
			break;
		}
	}

	@Override
    public void close() throws IOException {
		this.commandResources.close();
    }

}
