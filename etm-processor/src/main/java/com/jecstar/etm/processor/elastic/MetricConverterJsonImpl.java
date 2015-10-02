package com.jecstar.etm.processor.elastic;

import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.jecstar.etm.core.domain.converter.json.AbstractJsonConverter;

public class MetricConverterJsonImpl extends AbstractJsonConverter {

	@SuppressWarnings("rawtypes")
	public boolean appendGauges(Map<String, Gauge> gauges, StringBuilder buffer, boolean firstElement) {
        if (gauges == null || gauges.isEmpty()) {
        	return false;
        }
    	if (!firstElement) {
    		buffer.append(", ");
    	}
    	buffer.append("\"gauges\": [");
    	buffer.append(gauges.entrySet().stream()
    			.map(f -> "{\"name\": \"" + escapeToJson(f.getKey()) + "\", \"value\": \"" + escapeToJson(f.getValue().getValue().toString()) + "\"}")
    			.collect(Collectors.joining(", ")));
        buffer.append("]");
        return true;
	}
	
	public boolean appendCounters(Map<String, Counter> counters, StringBuilder buffer, boolean firstElement) {
        if (counters == null || counters.isEmpty()) {
        	return false;
        }
    	if (!firstElement) {
    		buffer.append(", ");
    	}
    	buffer.append("\"counters\": [");
    	buffer.append(counters.entrySet().stream()
    			.map(f -> "{\"name\": \"" + escapeToJson(f.getKey()) + "\", \"value\": " + f.getValue().getCount() + "}")
    			.collect(Collectors.joining(", ")));
        buffer.append("]");
        return true;
	}

	public boolean appendHistograms(Map<String, Histogram> histograms, StringBuilder buffer, boolean firstElement) {
		if (histograms == null || histograms.isEmpty()) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("\"histograms\": [");
		buffer.append(histograms.entrySet().stream()
				.map(f -> {
					Snapshot snapshot =  f.getValue().getSnapshot();
					return "{\"name\": \"" + escapeToJson(f.getKey()) + "\", \"values\": {" +
							"\"count\": " + f.getValue().getCount() +
							", \"min\": " + snapshot.getMin() +
							", \"max\": " + snapshot.getMax() +
							", \"mean\": " + snapshot.getMean() +
							", \"stddev\": " + snapshot.getStdDev() +
							", \"median\": " + snapshot.getMedian() +
							", \"75%%\": " + snapshot.get75thPercentile() +
							", \"95%%\": " + snapshot.get95thPercentile() +
							", \"98%%\": " + snapshot.get98thPercentile() +
							", \"99%%\": " + snapshot.get99thPercentile() +
							", \"99.9%%\": " + snapshot.get999thPercentile() +
							"}}";
					})
				.collect(Collectors.joining(", "))
				);
		buffer.append("]");
		return true;
	}

	public boolean appendMeters(Map<String, Meter> meters, StringBuilder buffer, boolean firstElement, TimeUnit rateUnit, TimeUnit durationUnit) {
		if (meters == null || meters.isEmpty()) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("\"meters\": [");
		buffer.append(meters.entrySet().stream()
				.map(f -> {
					return "{\"name\": \"" + escapeToJson(f.getKey()) + "\", \"values\": {" +
							"\"count\": " + f.getValue().getCount() +
							", \"mean_rate_events_per_" + calculateJsonRateUnit(rateUnit) + "\": " + convertRate(f.getValue().getMeanRate(), rateUnit) +
							", \"1_minute_rate_events_per_" + calculateJsonRateUnit(rateUnit) + "\": " + convertRate(f.getValue().getOneMinuteRate(), rateUnit) +
							", \"5_minute_rate_events_per_" + calculateJsonRateUnit(rateUnit) + "\": " + convertRate(f.getValue().getFiveMinuteRate(), rateUnit) +
							", \"15_minute_rate_events_per_" + calculateJsonRateUnit(rateUnit) + "\": " + convertRate(f.getValue().getFifteenMinuteRate(), rateUnit) +
							"}}";
					})
				.collect(Collectors.joining(", "))
				);
		buffer.append("]");
		return true;
	}
	
	public boolean appendTimers(SortedMap<String, Timer> timers, StringBuilder buffer, boolean firstElement, TimeUnit rateUnit, TimeUnit durationUnit) {
		if (timers == null || timers.isEmpty()) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("\"timers\": [");
		buffer.append(timers.entrySet().stream()
				.map(f -> {
					Snapshot snapshot =  f.getValue().getSnapshot();
					return "{\"name\": \"" + escapeToJson(f.getKey()) + "\", \"values\": {" +
							"\"count\": " + f.getValue().getCount() +
							", \"mean_rate_calls_per_" + calculateJsonRateUnit(rateUnit) + "\": " + convertRate(f.getValue().getMeanRate(), rateUnit) +
							", \"1_minute_rate_calls_per_" + calculateJsonRateUnit(rateUnit) + "\": " + convertRate(f.getValue().getOneMinuteRate(), rateUnit) +
							", \"5_minute_rate_calls_per_" + calculateJsonRateUnit(rateUnit) + "\": " + convertRate(f.getValue().getFiveMinuteRate(), rateUnit) +
							", \"15_minute_rate_calls_per_" + calculateJsonRateUnit(rateUnit) + "\": " + convertRate(f.getValue().getFifteenMinuteRate(), rateUnit) +
							
							", \"min_in_" + calculateJsonDurationUnit(durationUnit) + "\": " + convertDuration(snapshot.getMin(), durationUnit) +
							", \"max_in_" + calculateJsonDurationUnit(durationUnit) + "\": " + convertDuration(snapshot.getMax(), durationUnit) +
							", \"mean_in_" + calculateJsonDurationUnit(durationUnit) + "\": " + convertDuration(snapshot.getMean(), durationUnit) +
							", \"stddev_in_" + calculateJsonDurationUnit(durationUnit) + "\": " + convertDuration(snapshot.getStdDev(), durationUnit) +
							", \"median_in_" + calculateJsonDurationUnit(durationUnit) + "\": " + convertDuration(snapshot.getMedian(), durationUnit) +
							", \"75%%_in_" + calculateJsonDurationUnit(durationUnit) + "\": " + convertDuration(snapshot.get75thPercentile(), durationUnit) +
							", \"95%%_in_" + calculateJsonDurationUnit(durationUnit) + "\": " + convertDuration(snapshot.get95thPercentile(), durationUnit) +
							", \"98%%_in_" + calculateJsonDurationUnit(durationUnit) + "\": " + convertDuration(snapshot.get98thPercentile(), durationUnit) +
							", \"99%%_in_" + calculateJsonDurationUnit(durationUnit) + "\": " + convertDuration(snapshot.get99thPercentile(), durationUnit) +
							", \"99.9%%_in_" + calculateJsonDurationUnit(durationUnit) + "\": " + convertDuration(snapshot.get999thPercentile(), durationUnit) +
							"}}";
					})
				.collect(Collectors.joining(", "))
				);
		buffer.append("]");
		return true;
	}
	
    private double convertDuration(double duration, TimeUnit durationUnit) {
        return duration * (1.0 / durationUnit.toNanos(1));
    }

    private double convertRate(double rate, TimeUnit rateUnit) {
        return rate * rateUnit.toSeconds(1);
    }
    
    private String calculateJsonRateUnit(TimeUnit unit) {
        final String s = unit.toString().toLowerCase(Locale.US);
        return escapeToJson(s.substring(0, s.length() - 1));
    }
    
    private String calculateJsonDurationUnit(TimeUnit unit) {
    	return escapeToJson(unit.toString().toLowerCase(Locale.US));
    }


}
