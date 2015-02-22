package com.holster.etm.scheduler.retention;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BatchStatement.Type;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class EtmDataCleaner {

	private final Session session;
	private final BatchStatement batchStatement = new BatchStatement(Type.UNLOGGED);
	private final BatchStatement counterBatchStatement = new BatchStatement(Type.COUNTER);
	private final PreparedStatement selectDataRetentionStatement;
	private final PreparedStatement deleteTelemetryEventStatement;
	private final PreparedStatement deleteSourceIdCorrelationStatement;
	private final PreparedStatement deleteCorrelationDataStatement;
	private final PreparedStatement deleteEventOccurrencesStatement;
	private final PreparedStatement deleteApplicationCountersStatement;
	private final PreparedStatement deleteEventNameCountersStatement;
	private final PreparedStatement deleteApplicationEventCountersStatement;
	private final PreparedStatement deleteTransactionNameCountersStatement;
	private final PreparedStatement deleteMessagePerformancesStatement;
	private final PreparedStatement deleteMessageExpirtationsStatement;
	private final PreparedStatement deleteTransactionPerformancesStatement;
	private final PreparedStatement deleteTransactionSlasStatement;
	private final PreparedStatement deleteDataRetentionStatement;

	public EtmDataCleaner(final Session session) {
	    this.session = session;
	    this.selectDataRetentionStatement = this.session.prepare("select id, eventOccurrenceTimeunit, sourceId, partionKeySuffix, applicationName, eventName, transactionName, correlationData from data_retention where timeunit = ?");
	    this.deleteTelemetryEventStatement = this.session.prepare("delete from telemetry_event where id = ?");
	    this.deleteSourceIdCorrelationStatement = this.session.prepare("delete from sourceid_id_correlation where sourceId = ?");
	    this.deleteCorrelationDataStatement = this.session.prepare("delete from correlation_data where name_timeunit = ?");
	    this.deleteEventOccurrencesStatement = this.session.prepare("delete from event_occurrences where timeunit = ?");
	    this.deleteApplicationCountersStatement = this.session.prepare("delete from application_counter where application_timeunit = ?");
	    this.deleteApplicationEventCountersStatement = this.session.prepare("delete from application_event_counter where application_timeunit = ?");
	    this.deleteEventNameCountersStatement = this.session.prepare("delete from eventName_counter where eventName_timeunit = ?");
	    this.deleteTransactionNameCountersStatement = this.session.prepare("delete from transactionName_counter where transactionName_timeunit = ?");
	    this.deleteMessagePerformancesStatement = this.session.prepare("delete from message_performance where name_timeunit = ?");
	    this.deleteMessageExpirtationsStatement = this.session.prepare("delete from message_expiration where name_timeunit = ?");
	    this.deleteTransactionPerformancesStatement = this.session.prepare("delete from transaction_performance where transactionName_timeunit = ?");
	    this.deleteTransactionSlasStatement = this.session.prepare("delete from transaction_sla where transactionName_timeunit = ?");
	    this.deleteDataRetentionStatement = this.session.prepare("delete from data_retention where timeunit = ?");
    }

	public void cleanup(Date cleanupTime, boolean preserveEventCounts, boolean preserveEventPerformances, boolean preserveEventSlas) {
		Iterator<Row> rowIterator = this.session.execute(this.selectDataRetentionStatement.bind(cleanupTime)).iterator();
		List<Date> eventOccurrences = new ArrayList<Date>();
		List<String> correlationNames = new ArrayList<String>();
		List<String> applicationNames = new ArrayList<String>();
		List<String> eventNames = new ArrayList<String>();
		List<String> transactionNames = new ArrayList<String>();
		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			this.batchStatement.clear();
			final String partitionKeySuffix = row.getString(3);
			this.batchStatement.add(this.deleteTelemetryEventStatement.bind(row.getUUID(0)));
			String sourceId = row.getString(2);
			if (sourceId != null) {
				this.batchStatement.add(this.deleteSourceIdCorrelationStatement.bind(sourceId));
			}
			Map<String, String> correlationData = row.getMap(7, String.class, String.class);
			if (correlationData != null) {
				correlationData.forEach((k,v) -> {
					String key = k + partitionKeySuffix;
					if (!correlationNames.contains(key)) {
						correlationNames.add(key);
						this.batchStatement.add(this.deleteCorrelationDataStatement.bind(key));
					}
				});
			}
			final Date eventOccurrence = row.getDate(1);
			if (!eventOccurrences.contains(eventOccurrence)) {
				eventOccurrences.add(eventOccurrence);
				this.batchStatement.add(this.deleteEventOccurrencesStatement.bind(eventOccurrence));
			}
			
			String applicationName = row.getString(4);
			if (applicationName != null) {
				String key = applicationName + partitionKeySuffix;
				if (!applicationNames.contains(key)) {
					applicationNames.add(key);
					if (!preserveEventCounts) {
						this.counterBatchStatement.add(this.deleteApplicationCountersStatement.bind(key));
						this.counterBatchStatement.add(this.deleteApplicationEventCountersStatement.bind(key));
					}
				}
			}
			String eventName = row.getString(5);
			if (eventName != null) {
				String key = eventName + partitionKeySuffix;
				if (!eventNames.contains(key)) {
					eventNames.add(key);
					if (!preserveEventCounts) {
						this.counterBatchStatement.add(this.deleteEventNameCountersStatement.bind(key));
					}
					if (!preserveEventPerformances) {
						this.batchStatement.add(this.deleteMessagePerformancesStatement.bind(key));
						this.batchStatement.add(this.deleteMessageExpirtationsStatement.bind(key));
					}
				}
			}
			String transactionName = row.getString(6);
			if (transactionName != null) {
				String key = transactionName + partitionKeySuffix;
				if (!transactionNames.contains(key)) {
					transactionNames.add(key);
					if (!preserveEventCounts) {
						this.counterBatchStatement.add(this.deleteTransactionNameCountersStatement.bind(key));
					}
					if (!preserveEventPerformances) {
						this.batchStatement.add(this.deleteTransactionPerformancesStatement.bind(key));
					}
					if (!preserveEventSlas) {
						this.batchStatement.add(this.deleteTransactionSlasStatement.bind(key));
					}
				}
			}
			if (this.batchStatement.size() > 0) {
				this.session.executeAsync(this.batchStatement);
			}
			if (this.counterBatchStatement.size() > 0) {
				this.session.executeAsync(this.counterBatchStatement);
			}
		}
		this.session.executeAsync(this.deleteDataRetentionStatement.bind(cleanupTime));
    }
}
