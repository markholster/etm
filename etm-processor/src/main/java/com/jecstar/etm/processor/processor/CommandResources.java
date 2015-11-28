package com.jecstar.etm.processor.processor;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.client.Client;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.jecstar.etm.processor.processor.enhancing.TelemetryEventEnhancer;
import com.jecstar.etm.processor.processor.persisting.MessagingTelemetryEventPersister;
import com.jecstar.etm.processor.processor.persisting.TelemetryEventPersister;

public class CommandResources {

	@SuppressWarnings("rawtypes")
	private Map<CommandType, TelemetryEventPersister> persisters = new HashMap<CommandType, TelemetryEventPersister>();
	private Map<CommandType, TelemetryEventEnhancer> enhancer = new HashMap<CommandType, TelemetryEventEnhancer>();

	public CommandResources(final Client elasticClient, final EtmConfiguration etmConfiguration, final MetricRegistry metricRegistry) {
		this.persisters.put(CommandType.MESSAGING_EVENT, new MessagingTelemetryEventPersister(elasticClient, etmConfiguration, metricRegistry));
	}

	@SuppressWarnings("unchecked")
	<T> T getPersister(CommandType commandType) {
		return (T) persisters.get(commandType);
	}

	@SuppressWarnings("unchecked")
	<T> T getEnhancer(CommandType commandType) {
		return (T) enhancer.get(commandType);
	}

	public void close() {
		this.persisters.values().forEach(c -> {
			try {
				c.close();
			} catch (Exception e) {
				
			}
		});
	}
	
}
