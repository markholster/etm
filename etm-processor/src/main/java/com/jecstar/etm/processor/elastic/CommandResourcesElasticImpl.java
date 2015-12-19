package com.jecstar.etm.processor.elastic;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.configuration.ConfigurationChangeListener;
import com.jecstar.etm.core.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.jecstar.etm.processor.processor.CommandResources;
import com.jecstar.etm.processor.processor.enhancing.TelemetryEventEnhancer;
import com.jecstar.etm.processor.processor.persisting.AbstractTelemetryEventPersister;
import com.jecstar.etm.processor.processor.persisting.SqlTelemetryEventPersister;
import com.jecstar.etm.processor.processor.persisting.HttpTelemetryEventPersister;
import com.jecstar.etm.processor.processor.persisting.LogTelemetryEventPersister;
import com.jecstar.etm.processor.processor.persisting.MessagingTelemetryEventPersister;
import com.jecstar.etm.processor.processor.persisting.TelemetryEventPersister;

public class CommandResourcesElasticImpl implements CommandResources, ConfigurationChangeListener {

	private final Client elasticClient;
	private final EtmConfiguration etmConfiguration;
	private final BulkProcessorMetricLogger bulkProcessorMetricLogger;
	
	@SuppressWarnings("rawtypes")
	private Map<CommandType, TelemetryEventPersister> persisters = new HashMap<CommandType, TelemetryEventPersister>();
	@SuppressWarnings("rawtypes")
	private Map<CommandType, TelemetryEventEnhancer> enhancer = new HashMap<CommandType, TelemetryEventEnhancer>();
	
	private BulkProcessor bulkProcessor;

	public CommandResourcesElasticImpl(final Client elasticClient, final EtmConfiguration etmConfiguration, final MetricRegistry metricRegistry) {
		this.elasticClient = elasticClient;
		this.etmConfiguration = etmConfiguration;
		this.bulkProcessorMetricLogger = new BulkProcessorMetricLogger(metricRegistry);
		this.bulkProcessor = createBulkProcessor();
		this.etmConfiguration.addConfigurationChangeListener(this);
		this.persisters.put(CommandType.HTTP_EVENT, new HttpTelemetryEventPersister(this.bulkProcessor, etmConfiguration));
		this.persisters.put(CommandType.LOG_EVENT, new LogTelemetryEventPersister(this.bulkProcessor, etmConfiguration));
		this.persisters.put(CommandType.MESSAGING_EVENT, new MessagingTelemetryEventPersister(this.bulkProcessor, etmConfiguration));
		this.persisters.put(CommandType.SQL_EVENT, new SqlTelemetryEventPersister(this.bulkProcessor, etmConfiguration));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getPersister(CommandType commandType) {
		return (T) persisters.get(commandType);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getEnhancer(CommandType commandType) {
		return (T) enhancer.get(commandType);
	}

	@Override
	public void close() {
		this.etmConfiguration.removeConfigurationChangeListener(this);
		this.bulkProcessor.close();
	}
	
	private BulkProcessor createBulkProcessor() {
		return BulkProcessor.builder(this.elasticClient, this.bulkProcessorMetricLogger)
				.setBulkActions(this.etmConfiguration.getPersistingBulkCount() <= 0 ? -1 : this.etmConfiguration.getPersistingBulkCount())
				.setBulkSize(new ByteSizeValue(this.etmConfiguration.getPersistingBulkSize() <=0 ? -1 : this.etmConfiguration.getPersistingBulkSize(), ByteSizeUnit.KB))
				.setFlushInterval(this.etmConfiguration.getPersistingBulkTime() <= 0 ? null : TimeValue.timeValueMillis(this.etmConfiguration.getPersistingBulkTime()))
				.build();
	}

	@Override
	public void configurationChanged(ConfigurationChangedEvent event) {
		if (event.isChanged(EtmConfiguration.CONFIG_KEY_PERSISTING_BULK_SIZE)) {
			BulkProcessor oldBulkProcessor = this.bulkProcessor;
			this.bulkProcessor = createBulkProcessor();
			this.persisters.values().forEach(c -> ((AbstractTelemetryEventPersister)c).setBulkProcessor(this.bulkProcessor));
			oldBulkProcessor.close();
		}
	}
	
	private class BulkProcessorMetricLogger implements BulkProcessor.Listener {
		
		private final Timer bulkTimer;
		private Map<Long, Context> metricContext = new ConcurrentHashMap<Long, Context>();
		
		private BulkProcessorMetricLogger(final MetricRegistry metricRegistry) {
			this.bulkTimer = metricRegistry.timer("event-processor-persisting-repository-bulk-update");
		}
		
		@Override
		public void beforeBulk(long executionId, BulkRequest request) {
			this.metricContext.put(executionId, this.bulkTimer.time());
		}
		
		@Override
		public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
			this.metricContext.remove(executionId).stop();
		}
		
		@Override
		public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
			this.metricContext.remove(executionId).stop();
		}
		
	}
	
}
