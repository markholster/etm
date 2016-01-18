package com.jecstar.etm.processor.processor;

import java.io.Closeable;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.domain.converter.json.BusinessTelemetryEventConverterJsonImpl;
import com.jecstar.etm.core.domain.converter.json.HttpTelemetryEventConverterJsonImpl;
import com.jecstar.etm.core.domain.converter.json.LogTelemetryEventConverterJsonImpl;
import com.jecstar.etm.core.domain.converter.json.MessagingTelemetryEventConverterJsonImpl;
import com.jecstar.etm.core.domain.converter.json.SqlTelemetryEventConverterJsonImpl;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.processor.persisting.elastic.BusinessTelemetryEventPersister;
import com.jecstar.etm.processor.processor.persisting.elastic.HttpTelemetryEventPersister;
import com.jecstar.etm.processor.processor.persisting.elastic.LogTelemetryEventPersister;
import com.jecstar.etm.processor.processor.persisting.elastic.MessagingTelemetryEventPersister;
import com.jecstar.etm.processor.processor.persisting.elastic.SqlTelemetryEventPersister;
import com.lmax.disruptor.EventHandler;

public class PersistingEventHandler implements EventHandler<TelemetryCommand>, Closeable {

	private final long ordinal;
	private final long numberOfConsumers;
	private final Timer timer;
	private final CommandResources commandResources;
	
	private final BusinessTelemetryEventConverterJsonImpl businessTelemetryEventConverter = new BusinessTelemetryEventConverterJsonImpl();
	private final HttpTelemetryEventConverterJsonImpl httpTelemetryEventConverter = new HttpTelemetryEventConverterJsonImpl();
	private final LogTelemetryEventConverterJsonImpl logTelemetryEventConverter = new LogTelemetryEventConverterJsonImpl();
	private final MessagingTelemetryEventConverterJsonImpl messagingTelemetryEventConverter = new MessagingTelemetryEventConverterJsonImpl();
	private final SqlTelemetryEventConverterJsonImpl sqlTelemetryEventConverter = new SqlTelemetryEventConverterJsonImpl();

	public PersistingEventHandler(final long ordinal, final long numberOfConsumers, final CommandResources commandResources, final MetricRegistry metricRegistry) {
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		this.commandResources = commandResources;
		this.timer = metricRegistry.timer("event-processor.persisting");
	}

	@Override
	public void onEvent(TelemetryCommand command, long sequence, boolean endOfBatch) throws Exception {
		if (sequence % this.numberOfConsumers != this.ordinal) {
			return;
		}
		switch (command.commandType) {
		case BUSINESS_EVENT:	
			final BusinessTelemetryEventPersister businessPersister = this.commandResources.getPersister(command.commandType);
			final Context bsuineesTimerContext = this.timer.time();
			try {
				businessPersister.persist(command.businessTelemetryEvent, this.businessTelemetryEventConverter);
			} finally {
				bsuineesTimerContext.stop();
			}
			break;			
		case HTTP_EVENT:	
			final HttpTelemetryEventPersister httpPersister = this.commandResources.getPersister(command.commandType);
			final Context httpTimerContext = this.timer.time();
			try {
				httpPersister.persist(command.httpTelemetryEvent, this.httpTelemetryEventConverter);
			} finally {
				httpTimerContext.stop();
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
		case MESSAGING_EVENT:
			final MessagingTelemetryEventPersister messagingPersister = this.commandResources.getPersister(command.commandType);
			final Context messagingTimerContext = this.timer.time();
			try {
				messagingPersister.persist(command.messagingTelemetryEvent, this.messagingTelemetryEventConverter);
			} finally {
				messagingTimerContext.stop();
			}
			break;
		case SQL_EVENT:
			final SqlTelemetryEventPersister sqlPersister = this.commandResources.getPersister(command.commandType);
			final Context sqlTimerContext = this.timer.time();
			try {
				sqlPersister.persist(command.sqlTelemetryEvent, this.sqlTelemetryEventConverter);
			} finally {
				sqlTimerContext.stop();
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
