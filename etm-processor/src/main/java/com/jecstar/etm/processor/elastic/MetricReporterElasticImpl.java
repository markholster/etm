package com.jecstar.etm.processor.elastic;

import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.client.Client;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;

public class MetricReporterElasticImpl extends ScheduledReporter {

	private static final TimeUnit rateUnit = TimeUnit.SECONDS;
	private static final TimeUnit durationUnit = TimeUnit.MILLISECONDS;
	private final Client elasticClient;
	private MetricConverterJsonImpl metricConverter;

	public MetricReporterElasticImpl(MetricRegistry registry, String nodeName, Client elasticClient) {
		super(registry, nodeName, MetricFilter.ALL, rateUnit, durationUnit);
		this.elasticClient = elasticClient;
		this.metricConverter = new MetricConverterJsonImpl();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("{");
		buffer.append(""
				+ "\"timestamp\": " + System.currentTimeMillis());
		this.metricConverter.appendGauges(gauges, buffer, false);
		this.metricConverter.appendCounters(counters, buffer, false);
		this.metricConverter.appendHistograms(histograms, buffer, false);
		this.metricConverter.appendMeters(meters, buffer, false, rateUnit, durationUnit);
		this.metricConverter.appendTimers(timers, buffer, false, rateUnit, durationUnit);
        buffer.append("}");
        System.out.println(buffer.toString());
		
	}
	

}
