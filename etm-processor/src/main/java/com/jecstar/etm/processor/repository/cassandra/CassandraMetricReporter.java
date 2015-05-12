package com.jecstar.etm.processor.repository.cassandra;

import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BatchStatement.Type;
import com.datastax.driver.core.Session;
import com.jecstar.etm.core.cassandra.PartitionKeySuffixCreator;
import com.jecstar.etm.core.util.DateUtils;

public class CassandraMetricReporter extends ScheduledReporter {

	private final Clock clock;
	private final String nodeName;
	private final Session session;
	private final BatchStatement batchStatement = new BatchStatement(Type.UNLOGGED);
	private final TimeUnit statisticsTimeUnit;

	public CassandraMetricReporter(String nodeName, MetricRegistry registry, final Session session, TimeUnit statisticsTimeUnit) {
		super(registry, nodeName + "-etm-reporter", MetricFilter.ALL, TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS);
		this.clock = Clock.defaultClock();
		this.nodeName = nodeName;
		this.session = session;
		this.statisticsTimeUnit = statisticsTimeUnit;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms,
	        SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		this.batchStatement.clear();
		final long timestamp = this.clock.getTime();
		Date partitionKey = new Date(DateUtils.normalizeTime(timestamp, PartitionKeySuffixCreator.SMALLEST_TIMUNIT_UNIT.toMillis(1)));
		Date statisticsTime = new Date(DateUtils.normalizeTime(timestamp, this.statisticsTimeUnit.toMillis(1)));

		for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
			reportGauge(this.batchStatement, partitionKey, this.nodeName, statisticsTime, entry.getKey(), entry.getValue());
		}
		for (Map.Entry<String, Counter> entry : counters.entrySet()) {
			reportCounter(this.batchStatement, partitionKey, this.nodeName, statisticsTime, entry.getKey(), entry.getValue());
		}
		for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
			reportHistogram(this.batchStatement, partitionKey, this.nodeName, statisticsTime, entry.getKey(), entry.getValue());
		}
		for (Map.Entry<String, Meter> entry : meters.entrySet()) {
			reportMeter(this.batchStatement, partitionKey, this.nodeName, statisticsTime, entry.getKey(), entry.getValue());
		}
		for (Map.Entry<String, Timer> entry : timers.entrySet()) {
			reportTimer(this.batchStatement, partitionKey, this.nodeName, statisticsTime, entry.getKey(), entry.getValue());
		}
		this.session.executeAsync(this.batchStatement);
	}

	private void reportTimer(BatchStatement batchStatement, Date partitionKey, String nodeName, Date statisticsTime, String key,
            Timer value) {
	    // TODO Auto-generated method stub
	    
    }

	private void reportMeter(BatchStatement batchStatement, Date partitionKey, String nodeName, Date statisticsTime, String key,
            Meter value) {
	    // TODO Auto-generated method stub
	    
    }

	private void reportHistogram(BatchStatement batchStatement, Date partitionKey, String nodeName, Date statisticsTime, String key,
            Histogram value) {
	    // TODO Auto-generated method stub
	    
    }

	private void reportCounter(BatchStatement batchStatement, Date partitionKey, String nodeName, Date statisticsTime, String key,
            Counter value) {
	    // TODO Auto-generated method stub
	    
    }

	private void reportGauge(BatchStatement batchStatement, Date partitionKey, String nodeName, Date statisticsTime, String key,
            Gauge<?> value) {
	    // TODO Auto-generated method stub
	    
    }
}
