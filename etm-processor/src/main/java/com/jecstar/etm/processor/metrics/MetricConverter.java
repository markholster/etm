package com.jecstar.etm.processor.metrics;

import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

public interface MetricConverter<T> {

	@SuppressWarnings("rawtypes")
	T convertGauges(SortedMap<String, Gauge> gauges);
	
	T convertCounters(SortedMap<String, Counter> counters);
	
	T convertHistograms(SortedMap<String, Histogram> histograms);
	
	T convertMeters(SortedMap<String, Meter> meters, TimeUnit rateUnit);
	
	T convertTimers(SortedMap<String, Timer> timers, TimeUnit rateUnit, TimeUnit durationUnit);
	
	MetricConverterTags getTags();
}
