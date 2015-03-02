package com.jecstar.etm.processor.processor;

import java.nio.channels.IllegalSelectorException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.solr.client.solrj.SolrServer;

import com.datastax.driver.core.Session;
import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.TelemetryEventType;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.repository.CorrelationBySourceIdResult;
import com.jecstar.etm.processor.repository.EndpointConfigResult;
import com.jecstar.etm.processor.repository.StatementExecutor;
import com.lmax.disruptor.RingBuffer;

public class TelemetryEventProcessor {
	
	private RingBuffer<TelemetryEvent> ringBuffer;
	private boolean started = false;
	
	//TODO proberen dit niet in een synchronised map te plaatsen, maar bijvoorbeeld in een ConcurrentMap
	private final Map<String, CorrelationBySourceIdResult> sourceCorrelations = Collections.synchronizedMap(new HashMap<String, CorrelationBySourceIdResult>());
	
	private ExecutorService executorService;
	private Session cassandraSession;
	private SolrServer solrServer;
	private EtmConfiguration etmConfiguration;
	
	private DisruptorEnvironment disruptorEnvironment;
	private StatementExecutor statementExecutor;
	

	public void start(final ExecutorService executorService, final Session session, final SolrServer solrServer, final EtmConfiguration etmConfiguration) {
		if (this.started) {
			throw new IllegalStateException();
		}
		this.started = true;
		this.executorService = executorService;
		this.cassandraSession = session;
		this.statementExecutor = new StatementExecutor(this.cassandraSession);
		this.solrServer = solrServer;
		this.etmConfiguration = etmConfiguration;
		this.disruptorEnvironment = new DisruptorEnvironment(etmConfiguration, executorService, session, solrServer, this.statementExecutor, this.sourceCorrelations);
		this.ringBuffer = this.disruptorEnvironment.start();
	}
	
	public void hotRestart() {
		if (!this.started) {
			throw new IllegalStateException();
		}
		DisruptorEnvironment newDisruptorEnvironment = new DisruptorEnvironment(this.etmConfiguration, this.executorService, this.cassandraSession, this.solrServer, this.statementExecutor, this.sourceCorrelations);
		RingBuffer<TelemetryEvent> newRingBuffer = newDisruptorEnvironment.start();
		DisruptorEnvironment oldDisruptorEnvironment = this.disruptorEnvironment;
		
		this.ringBuffer = newRingBuffer;
		this.disruptorEnvironment = newDisruptorEnvironment;
		oldDisruptorEnvironment.shutdown();
	}
	
	public void stop() {
		if (!this.started) {
			throw new IllegalSelectorException();
		}
		this.disruptorEnvironment.shutdown();
	}
	
	public void stopAll() {
		if (!this.started) {
			throw new IllegalSelectorException();
		}		
		this.executorService.shutdown();
		this.disruptorEnvironment.shutdown();
		this.cassandraSession.close();
		this.solrServer.shutdown();
	}


	public void processTelemetryEvent(final TelemetryEvent telemetryEvent) {
		if (!this.started) {
			throw new IllegalSelectorException();
		}
		if (this.etmConfiguration.getLicenseExpriy().getTime() < System.currentTimeMillis()) {
			throw new EtmException(EtmException.LICENSE_EXPIRED_EXCEPTION);
		}
		long sequence = this.ringBuffer.next();
		try {
			TelemetryEvent target = this.ringBuffer.get(sequence);
			target.initialize(telemetryEvent);
			preProcess(target);
		} finally {
			this.ringBuffer.publish(sequence);
		}
	}
	
	private void preProcess(TelemetryEvent event) {
//		long start = System.nanoTime();
		if (event.creationTime.getTime() == 0) {
			event.creationTime.setTime(System.currentTimeMillis());
		}
		if (event.retention.getTime() == 0) {
			// Retention time should actually be the current time added with the
			// configured retention time, but that would cause performance
			// problems while cleaning up: Now, the partition key is removed, so
			// everything with the creation time in the same hour (the partition
			// key) is removed with one statement. If the actual addition time
			// is taken into account each and every row should be deleted one by
			// one. This would cause a huge performance degradation.
			event.retention.setTime(event.creationTime.getTime() + this.etmConfiguration.getDataRetentionTime());
		}
		if ((event.transactionName == null || event.name == null)&& TelemetryEventType.MESSAGE_REQUEST.equals(event.type)) {
			// A little bit of enhancement before the event is processed by the
			// disruptor. We need to make sure the eventName & transactionName
			// is determined because the correlation of the transaction data on
			// the response will fail if the request and response are processed
			// at exactly the same time. By determining the event name &
			// transaction name at this point, the only requirement is that the
			// request is offered to ETM before the response, which is quite
			// logical.
			EndpointConfigResult result = new EndpointConfigResult();
			this.disruptorEnvironment.findEndpointConfig(event.endpoint, result, this.etmConfiguration.getEndpointCacheExpiryTime());
			if (result.eventNameParsers != null && result.eventNameParsers.size() > 0) {
				event.name = parseValue(result.eventNameParsers, event.content);
			}
			if (result.transactionNameParsers != null && result.transactionNameParsers.size() > 0) {
				event.transactionName = parseValue(result.transactionNameParsers, event.content);
			}
			if (event.transactionName != null) {
				event.slaRule = result.slaRules.get(event.transactionName);
			}
		}
		if (event.transactionName != null) {
			event.transactionId = event.id;
		}
		if (event.sourceId != null) {
			this.sourceCorrelations.put(event.sourceId, new CorrelationBySourceIdResult(event.id, event.name, event.transactionId,
			        event.transactionName, event.creationTime.getTime(), event.expiryTime.getTime(), event.slaRule));
		}
//		Statistics.preprocessingTime.addAndGet(System.nanoTime() - start);
	}
	
	private String parseValue(List<ExpressionParser> expressionParsers, String content) {
		if (content == null || expressionParsers == null) {
			return null;
		}
		for (ExpressionParser expressionParser : expressionParsers) {
			String value = parseValue(expressionParser, content);
			if (value != null) {
				return value;
			}
		}
		return null;
    }
	
	private String parseValue(ExpressionParser expressionParser, String content) {
		if (expressionParser == null || content == null) {
			return null;
		}
		String value = expressionParser.evaluate(content);
		if (value != null && value.trim().length() > 0) {
			return value;
		}
		return null;
	}
}
