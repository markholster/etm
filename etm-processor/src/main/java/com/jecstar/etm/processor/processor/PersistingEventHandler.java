package com.jecstar.etm.processor.processor;

import java.io.Closeable;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.domain.converter.json.LogTelemetryEventConverterJsonImpl;
import com.jecstar.etm.core.domain.converter.json.MessagingTelemetryEventConverterJsonImpl;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.processor.persisting.elastic.LogTelemetryEventPersister;
import com.jecstar.etm.processor.processor.persisting.elastic.MessagingTelemetryEventPersister;
import com.lmax.disruptor.EventHandler;

public class PersistingEventHandler implements EventHandler<TelemetryCommand>, Closeable {

	private final long ordinal;
	private final long numberOfConsumers;
	private final Timer timer;
	private final CommandResources commandResources;
	private final MessagingTelemetryEventConverterJsonImpl messagingTelemetryEventConverter = new MessagingTelemetryEventConverterJsonImpl();
	private final LogTelemetryEventConverterJsonImpl logTelemetryEventConverter = new LogTelemetryEventConverterJsonImpl();

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
			final MessagingTelemetryEventPersister messagingPersister = this.commandResources.getPersister(command.commandType);
			final Context messaginTgimerContext = this.timer.time();
			try {
				messagingPersister.persist(command.messagingTelemetryEvent, this.messagingTelemetryEventConverter);
			} finally {
				messaginTgimerContext.stop();
			}
			break;
		case LOG_EVENT:	
			final LogTelemetryEventPersister logPersister = this.commandResources.getPersister(command.commandType);
			final Context logTimerContext = this.timer.time();
			try {
				logPersister.persist(command.logTelemetryEvent, this.logTelemetryEventConverter);
			} finally {
				logTimerContext.stop();
			}
			break;			
		default:
			throw new IllegalArgumentException("'" + command.commandType.name() + "' not implemented.");
		}
	}

	@Override
    public void close() {
    }

}
