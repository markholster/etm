package com.jecstar.etm.processor.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OperatingSystemMetricSet implements MetricSet {

	private final OperatingSystemMXBean os;

    public OperatingSystemMetricSet() {
		this.os = ManagementFactory.getOperatingSystemMXBean();
	}
    
	@Override
	public Map<String, Metric> getMetrics() {
		final Map<String, Metric> gauges = new HashMap<>();
        gauges.put("os.cpu.system_load_average", (Gauge<Double>) os::getSystemLoadAverage);
        return Collections.unmodifiableMap(gauges);
	}

}
