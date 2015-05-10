package com.jecstar.etm.processor.jee.configurator.cassandra;

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
import com.datastax.driver.core.Session;

public class CassandraMetricsReporter extends ScheduledReporter {

	private final Clock clock;
	private final String nodeName;
	private final Session session;

	public CassandraMetricsReporter(String nodeName, MetricRegistry registry, final Session session) {
		super(registry, nodeName + "-etm-reporter", MetricFilter.ALL, TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS);
		this.clock = Clock.defaultClock();
		this.nodeName = nodeName;
		this.session = session;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms,
	        SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		final long timestamp = TimeUnit.MILLISECONDS.toSeconds(this.clock.getTime());

		for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
			reportGauge(timestamp, entry.getKey(), entry.getValue());
		}
		for (Map.Entry<String, Counter> entry : counters.entrySet()) {
			reportCounter(timestamp, entry.getKey(), entry.getValue());
		}
		for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
			reportHistogram(timestamp, entry.getKey(), entry.getValue());
		}
		for (Map.Entry<String, Meter> entry : meters.entrySet()) {
			reportMeter(timestamp, entry.getKey(), entry.getValue());
		}
		for (Map.Entry<String, Timer> entry : timers.entrySet()) {
			reportTimer(timestamp, entry.getKey(), entry.getValue());
		}
	}

	private void reportTimer(long timestamp, String key, Timer value) {
	    // TODO Auto-generated method stub
	    
    }

	private void reportMeter(long timestamp, String key, Meter value) {
	    // TODO Auto-generated method stub
	    
    }

	private void reportHistogram(long timestamp, String key, Histogram value) {
	    // TODO Auto-generated method stub
	    
    }

	private void reportCounter(long timestamp, String key, Counter value) {
	    // TODO Auto-generated method stub
	    
    }

	private void reportGauge(long timestamp, String key, Gauge value) {
	    // TODO Auto-generated method stub
	    
    }

}
