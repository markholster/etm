package com.jecstar.etm.launcher.slf4j;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkProcessor.Listener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;

import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.domain.writers.json.LogTelemetryEventWriterJsonImpl;
import com.jecstar.etm.processor.core.persisting.elastic.LogTelemetryEventPersister;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

public class LogBulkProcessorWrapper implements Closeable {

	private static final LogTelemetryEventWriterJsonImpl writer = new LogTelemetryEventWriterJsonImpl();
	
	private EtmConfiguration configuration;
	private BulkProcessor bulkProcessor;
	
	private LogTelemetryEventPersister persister;
	
	private List<LogTelemetryEvent> startupBuffer = new ArrayList<>();
	
	private boolean open = true;

	public void setConfiguration(EtmConfiguration configuration) {
		this.configuration = configuration;
		if (this.persister == null && this.bulkProcessor != null) {
			this.persister = new LogTelemetryEventPersister(this.bulkProcessor, this.configuration);
			flushStartupBuffer();
		}
	}

	public void setClient(Client elasticClient) {
		this.bulkProcessor = BulkProcessor.builder(elasticClient, new Listener() {
			@Override
			public void beforeBulk(long executionId, BulkRequest request) {
			}
			@Override
			public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
			}
			@Override
			public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
			}}
		)
		.setBulkActions(100)
		.setBulkSize(new ByteSizeValue(1, ByteSizeUnit.MB))
		.setFlushInterval(TimeValue.timeValueSeconds(10))
		.build();
		if (this.persister == null && this.configuration != null) {
			this.persister = new LogTelemetryEventPersister(this.bulkProcessor, this.configuration);
			flushStartupBuffer();
		}
	}
	
	private synchronized void flushStartupBuffer() {
		for (LogTelemetryEvent event : this.startupBuffer) {
			persist(event);
		}
		this.startupBuffer.clear();
	}
	
	public void persist(LogTelemetryEvent event) {
		if (this.persister != null && isOpen()) {
			this.persister.persist(event, writer);
		} else {
			this.startupBuffer.add(event);
		}
	}
	
	private boolean isOpen() {
		return this.open;
	}

	@Override
	public void close() {
		this.open = false;
		if (this.bulkProcessor != null) {
			this.bulkProcessor.close();
		}
	}

	
	
}
