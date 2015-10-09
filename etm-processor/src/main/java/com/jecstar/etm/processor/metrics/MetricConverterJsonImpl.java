package com.jecstar.etm.processor.metrics;

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
import com.jecstar.etm.processor.metrics.MetricConverterTags.RateType;

public class MetricConverterJsonImpl extends AbstractJsonConverter implements MetricConverter<String>{
	
	private final MetricConverterTags tags = new MetricConverterTagsJsonImpl();
	
	@SuppressWarnings("rawtypes")
	@Override
	public String convertGauges(SortedMap<String, Gauge> gauges) {
		StringBuilder buffer = new StringBuilder();
    	buffer.append("\"" + this.tags.getGaugesTag() + "\": [");
    	buffer.append(gauges.entrySet().stream()
    			.map(f -> "{\"" + this.tags.getNameTag() + "\": \"" + escapeToJson(f.getKey()) + "\", \"" + this.tags.getValueTag() + "\": \"" + escapeToJson(f.getValue().getValue().toString()) + "\"}")
    			.collect(Collectors.joining(", ")));
        buffer.append("]");
		return buffer.toString();
	}

	@Override
	public String convertCounters(SortedMap<String, Counter> counters) {
		StringBuilder buffer = new StringBuilder();
    	buffer.append("\"" + this.tags.getCountersTag() + "\": [");
    	buffer.append(counters.entrySet().stream()
    			.map(f -> "{\"" + this.tags.getNameTag() + "\": \"" + escapeToJson(f.getKey()) + "\", \"" + this.tags.getValueTag() + "\": " + f.getValue().getCount() + "}")
    			.collect(Collectors.joining(", ")));
        buffer.append("]");
		return buffer.toString();
	}

	@Override
	public String convertHistograms(SortedMap<String, Histogram> histograms) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("\"" + this.tags.getHistogramsTag() + "\": [");
		buffer.append(histograms.entrySet().stream()
				.map(f -> {
					Snapshot snapshot =  f.getValue().getSnapshot();
					return "{\"" + this.tags.getNameTag() + "\": \"" + escapeToJson(f.getKey()) + "\", \"" + this.tags.getValuesTag() + "\": {" +
							"\"" + this.tags.getCountTag() + "\": " + f.getValue().getCount() +
							", \"" + this.tags.getMinTag() + "\": " + snapshot.getMin() +
							", \"" + this.tags.getMaxTag() + "\": " + snapshot.getMax() +
							", \"" + this.tags.getMeanTag() + "\": " + snapshot.getMean() +
							", \"" + this.tags.getStandardDeviationTag() +"\": " + snapshot.getStdDev() +
							", \"" + this.tags.getMedianTag() + "\": " + snapshot.getMedian() +
							", \"" + this.tags.get75thPercentileTag() + "\": " + snapshot.get75thPercentile() +
							", \"" + this.tags.get95thPercentileTag() + "\": " + snapshot.get95thPercentile() +
							", \"" + this.tags.get98thPercentileTag() + "\": " + snapshot.get98thPercentile() +
							", \"" + this.tags.get99thPercentileTag() + "\": " + snapshot.get99thPercentile() +
							", \"" + this.tags.get999thPercentileTag() + "\": " + snapshot.get999thPercentile() +
							"}}";
					})
				.collect(Collectors.joining(", "))
				);
		buffer.append("]");
		return buffer.toString();
	}

	@Override
	public String convertMeters(SortedMap<String, Meter> meters, TimeUnit rateUnit) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("\"" + this.tags.getMetersTag() + "\": [");
		buffer.append(meters.entrySet().stream()
				.map(f -> {
					return "{\"" + this.tags.getNameTag() + "\": \"" + escapeToJson(f.getKey()) + "\", \"" + this.tags.getValuesTag() + "\": {" +
							"\"" + this.tags.getCountTag() + "\": " + f.getValue().getCount() +
							", \"" + this.tags.getMeanRateTag(RateType.EVENTS, rateUnit) + "\": " + convertRate(f.getValue().getMeanRate(), rateUnit) +
							", \"" + this.tags.getOneMinuteRateTag(RateType.EVENTS, rateUnit) + "\": " + convertRate(f.getValue().getOneMinuteRate(), rateUnit) +
							", \"" + this.tags.getFiveMinuteRateTag(RateType.EVENTS, rateUnit) + "\": " + convertRate(f.getValue().getFiveMinuteRate(), rateUnit) +
							", \"" + this.tags.getFifteenMinuteRateTag(RateType.EVENTS, rateUnit) + "\": " + convertRate(f.getValue().getFifteenMinuteRate(), rateUnit) +
							"}}";
					})
				.collect(Collectors.joining(", "))
				);
		buffer.append("]");
		return buffer.toString();
	}

	@Override
	public String convertTimers(SortedMap<String, Timer> timers, TimeUnit rateUnit, TimeUnit durationUnit) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("\"" + this.tags.getTimersTag() + "\": [");
		buffer.append(timers.entrySet().stream()
				.map(f -> {
					Snapshot snapshot =  f.getValue().getSnapshot();
					return "{\"" + this.tags.getNameTag() + "\": \"" + escapeToJson(f.getKey()) + "\", \"" + this.tags.getValuesTag() + "\": {" +
							"\"" + this.tags.getCountTag() + "\": " + f.getValue().getCount() +
							", \"" + this.tags.getMeanRateTag(RateType.CALLS, rateUnit) + "\": " + convertRate(f.getValue().getMeanRate(), rateUnit) +
							", \"" + this.tags.getOneMinuteRateTag(RateType.CALLS, rateUnit) + "\": " + convertRate(f.getValue().getOneMinuteRate(), rateUnit) +
							", \"" + this.tags.getFiveMinuteRateTag(RateType.CALLS, rateUnit) + "\": " + convertRate(f.getValue().getFiveMinuteRate(), rateUnit) +
							", \"" + this.tags.getFifteenMinuteRateTag(RateType.CALLS, rateUnit) + "\": " + convertRate(f.getValue().getFifteenMinuteRate(), rateUnit) +
							
							", \"" + this.tags.getMinDurationTag(durationUnit) + "\": " + convertDuration(snapshot.getMin(), durationUnit) +
							", \"" + this.tags.getMaxDurationTag(durationUnit) + "\": " + convertDuration(snapshot.getMax(), durationUnit) +
							", \"" + this.tags.getMeanDurationTag(durationUnit) + "\": " + convertDuration(snapshot.getMean(), durationUnit) +
							", \"" + this.tags.getStandardDeviationDurationTag(durationUnit) + "\": " + convertDuration(snapshot.getStdDev(), durationUnit) +
							", \"" + this.tags.getMedianDurationTag(durationUnit) + "\": " + convertDuration(snapshot.getMedian(), durationUnit) +
							", \"" + this.tags.get75thPercentileDurationTag(durationUnit) + "\": " + convertDuration(snapshot.get75thPercentile(), durationUnit) +
							", \"" + this.tags.get95thPercentileDurationTag(durationUnit) + "\": " + convertDuration(snapshot.get95thPercentile(), durationUnit) +
							", \"" + this.tags.get98thPercentileDurationTag(durationUnit) + "\": " + convertDuration(snapshot.get98thPercentile(), durationUnit) +
							", \"" + this.tags.get99thPercentileDurationTag(durationUnit) + "\": " + convertDuration(snapshot.get99thPercentile(), durationUnit) +
							", \"" + this.tags.get999thPercentileDurationTag(durationUnit) + "\": " + convertDuration(snapshot.get999thPercentile(), durationUnit) +
							"}}";
					})
				.collect(Collectors.joining(", "))
				);
		buffer.append("]");
		return buffer.toString();
	}
	
    private double convertDuration(double duration, TimeUnit durationUnit) {
        return duration * (1.0 / durationUnit.toNanos(1));
    }

    private double convertRate(double rate, TimeUnit rateUnit) {
        return rate * rateUnit.toSeconds(1);
    }

	@Override
	public MetricConverterTags getTags() {
		return this.tags;
	}
}
