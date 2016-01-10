package com.jecstar.etm.processor.elastic;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.client.Client;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.metrics.MetricConverterTags;
import com.jecstar.etm.processor.metrics.MetricConverterTags.RateType;
import com.jecstar.etm.processor.metrics.MetricConverterTagsJsonImpl;

public class MetricReporterElasticImpl extends ScheduledReporter {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(MetricReporterElasticImpl.class);

	private static final TimeUnit rateUnit = TimeUnit.SECONDS;
	private static final TimeUnit durationUnit = TimeUnit.MILLISECONDS;
	private final Client elasticClient;
	private final String nodeName;
	private final String componentType;
	
	private final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.YEAR, 4)
			.appendLiteral("-")
			.appendValue(ChronoField.MONTH_OF_YEAR, 2)
			.appendLiteral("-")
			.appendValue(ChronoField.DAY_OF_MONTH, 2).toFormatter().withZone(ZoneId.of("UTC"));
	private final MetricConverterTags tags = new MetricConverterTagsJsonImpl();

	public MetricReporterElasticImpl(MetricRegistry registry, String nodeName, String componentType, Client elasticClient) {
		super(registry, componentType + "@" + nodeName, MetricFilter.ALL, rateUnit, durationUnit);
		this.elasticClient = elasticClient;
		this.nodeName = nodeName;
		this.componentType = componentType;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		final ObjectMapper objectMapper = new ObjectMapper();
		
		Instant now = Instant.now();
		SortedMap<String, Object> root = new TreeMap<String, Object>();
		root.put(this.tags.getTimestampTag(), now.toEpochMilli());
		appendNodeInfo(root);
		if (gauges != null && !gauges.isEmpty()) {
			addGauges(root, gauges);
		}
		if (counters != null && !counters.isEmpty()) {
			addCounters(root, counters);
		}
		if (histograms != null && !histograms.isEmpty()) {
			addHistograms(root, histograms);
		}
		if (meters != null && !meters.isEmpty()) {
			addMeters(root, meters, rateUnit);
		}
		if (timers != null && !timers.isEmpty()) {
			addTimers(root, timers, rateUnit, durationUnit);
		}
		
		try (StringWriter sw = new StringWriter()){
			objectMapper.writeValue(sw, root);
	        this.elasticClient.prepareIndex(getElasticIndexName(now), this.componentType, "" + now.toEpochMilli())
        	.setConsistencyLevel(WriteConsistencyLevel.ONE)
        	.setSource(sw.toString()).get();
		} catch (IOException e) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Failed to generate json metrics", e);
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	private void addGauges(SortedMap<String, Object> root, SortedMap<String, Gauge> gauges) {
		for (Entry<String, Gauge> entry : gauges.entrySet()) {
			Map<String, Object> valueMap = getValueMap(root, entry.getKey());
			String name = getValueKey(entry.getKey());
			valueMap.put(name, entry.getValue().getValue());
		}
	}
	
	private void addCounters(SortedMap<String, Object> root, SortedMap<String, Counter> counters) {
		for (Entry<String, Counter> entry : counters.entrySet()) {
			Map<String, Object> valueMap = getValueMap(root, entry.getKey());
			String name = getValueKey(entry.getKey());
			valueMap.put(name, entry.getValue().getCount());
		}
	}
	
	@SuppressWarnings("unchecked")
	private void addHistograms(SortedMap<String, Object> root, SortedMap<String, Histogram> histograms) {
		for (Entry<String, Histogram> entry : histograms.entrySet()) {
			Map<String, Object> valueMap = getValueMap(root, entry.getKey());
			String name = getValueKey(entry.getKey());
			if (valueMap.containsKey(name)) {
				valueMap = (Map<String, Object>) valueMap.get(name);
			} else {
				Map<String, Object> map = new TreeMap<String, Object>();
				valueMap.put(name, map);
				valueMap = map;
			}
			Snapshot snapshot =  entry.getValue().getSnapshot();
			valueMap.put(this.tags.getCountTag(), entry.getValue().getCount());
			valueMap.put(this.tags.getMinTag(), snapshot.getMin());
			valueMap.put(this.tags.getMaxTag(), snapshot.getMax());
			valueMap.put(this.tags.getMeanTag(), snapshot.getMean());
			valueMap.put(this.tags.getStandardDeviationTag(), snapshot.getStdDev());
			valueMap.put(this.tags.getMedianTag(), snapshot.getMedian());
			valueMap.put(this.tags.get75thPercentileTag(), snapshot.get75thPercentile());
			valueMap.put(this.tags.get95thPercentileTag(), snapshot.get95thPercentile());
			valueMap.put(this.tags.get98thPercentileTag(), snapshot.get98thPercentile());
			valueMap.put(this.tags.get99thPercentileTag(), snapshot.get99thPercentile());
			valueMap.put(this.tags.get999thPercentileTag(), snapshot.get999thPercentile());
		}
	}
	
	@SuppressWarnings("unchecked")
	private void addMeters(SortedMap<String, Object> root, SortedMap<String, Meter> meters, TimeUnit rateUnit) {
		for (Entry<String, Meter> entry : meters.entrySet()) {
			Map<String, Object> valueMap = getValueMap(root, entry.getKey());
			String name = getValueKey(entry.getKey());
			if (valueMap.containsKey(name)) {
				valueMap = (Map<String, Object>) valueMap.get(name);
			} else {
				Map<String, Object> map = new TreeMap<String, Object>();
				valueMap.put(name, map);
				valueMap = map;
			}
			valueMap.put(this.tags.getCountTag(), entry.getValue().getCount());
			valueMap.put(this.tags.getMeanRateTag(RateType.EVENTS, rateUnit), convertRate(entry.getValue().getMeanRate(), rateUnit));
			valueMap.put(this.tags.getOneMinuteRateTag(RateType.EVENTS, rateUnit), convertRate(entry.getValue().getOneMinuteRate(), rateUnit));
			valueMap.put(this.tags.getFiveMinuteRateTag(RateType.EVENTS, rateUnit), convertRate(entry.getValue().getFiveMinuteRate(), rateUnit));
			valueMap.put(this.tags.getFifteenMinuteRateTag(RateType.EVENTS, rateUnit), convertRate(entry.getValue().getFifteenMinuteRate(), rateUnit));
		}	
	}
	
	@SuppressWarnings("unchecked")
	private void addTimers(SortedMap<String, Object> root, SortedMap<String, Timer> timers, TimeUnit rateUnit, TimeUnit durationUnit) {
		for (Entry<String, Timer> entry : timers.entrySet()) {
			Map<String, Object> valueMap = getValueMap(root, entry.getKey());
			String name = getValueKey(entry.getKey());
			if (valueMap.containsKey(name)) {
				valueMap = (Map<String, Object>) valueMap.get(name);
			} else {
				Map<String, Object> map = new TreeMap<String, Object>();
				valueMap.put(name, map);
				valueMap = map;
			}
			Snapshot snapshot =  entry.getValue().getSnapshot();
			valueMap.put(this.tags.getCountTag(), entry.getValue().getCount());
			valueMap.put(this.tags.getMeanRateTag(RateType.CALLS, rateUnit), convertRate(entry.getValue().getMeanRate(), rateUnit));
			valueMap.put(this.tags.getOneMinuteRateTag(RateType.CALLS, rateUnit), convertRate(entry.getValue().getOneMinuteRate(), rateUnit));
			valueMap.put(this.tags.getFiveMinuteRateTag(RateType.CALLS, rateUnit), convertRate(entry.getValue().getFiveMinuteRate(), rateUnit));
			valueMap.put(this.tags.getFifteenMinuteRateTag(RateType.CALLS, rateUnit), convertRate(entry.getValue().getFifteenMinuteRate(), rateUnit));
			
			valueMap.put(this.tags.getMinDurationTag(durationUnit), convertDuration(snapshot.getMin(), durationUnit));
			valueMap.put(this.tags.getMaxDurationTag(durationUnit), convertDuration(snapshot.getMax(), durationUnit));
			valueMap.put(this.tags.getMeanDurationTag(durationUnit), convertDuration(snapshot.getMean(), durationUnit));
			valueMap.put(this.tags.getStandardDeviationDurationTag(durationUnit), convertDuration(snapshot.getStdDev(), durationUnit));
			valueMap.put(this.tags.getMedianDurationTag(durationUnit), convertDuration(snapshot.getMedian(), durationUnit));
			valueMap.put(this.tags.get75thPercentileDurationTag(durationUnit), convertDuration(snapshot.get75thPercentile(), durationUnit));
			valueMap.put(this.tags.get95thPercentileDurationTag(durationUnit), convertDuration(snapshot.get95thPercentile(), durationUnit));
			valueMap.put(this.tags.get98thPercentileDurationTag(durationUnit), convertDuration(snapshot.get98thPercentile(), durationUnit));
			valueMap.put(this.tags.get99thPercentileDurationTag(durationUnit), convertDuration(snapshot.get99thPercentile(), durationUnit));
			valueMap.put(this.tags.get999thPercentileDurationTag(durationUnit), convertDuration(snapshot.get999thPercentile(), durationUnit));
		}	
	}


	@SuppressWarnings("unchecked")
	private SortedMap<String, Object> getValueMap(SortedMap<String, Object> parent, String key) {
		String[] split = key.split("\\.");
		if (split.length == 1) {
			return parent;
		}
		String name = split[0];
		SortedMap<String, Object> result = null;
		if (parent.containsKey(name)) {
			result = (SortedMap<String, Object>) parent.get(name);
		} else {
			result = new TreeMap<String, Object>();
			parent.put(name, result);
		}
		return getValueMap(result, key.substring(name.length() + 1)); 
	}
	
	private String getValueKey(String key) {
		String[] split = key.split("\\.");
		return split[split.length -1];
	}
	
    private double convertDuration(double duration, TimeUnit durationUnit) {
        return duration * (1.0 / durationUnit.toNanos(1));
    }

    private double convertRate(double rate, TimeUnit rateUnit) {
        return rate * rateUnit.toSeconds(1);
    }

	
	private void appendNodeInfo(Map<String, Object> root) {
		Map<String, Object> nodeMap = new LinkedHashMap<String, Object>();
		root.put(this.tags.getNodeTag(), nodeMap);
		nodeMap.put(this.tags.getNameTag(), this.nodeName);
		nodeMap.put(this.tags.getComponentTag(), this.componentType);
	}

	/**
	 * Gives the name of the elastic index of the given
	 * <code>TelemetryEvent</code>.
	 * 
	 * @param event
	 *            The <code>TelemetryEvent</code> to determine the elastic index
	 *            name from.
	 * @return The name of the index.
	 */
	public String getElasticIndexName(Instant instant) {
		return "etm_stats_" + this.dateTimeFormatter.format(instant);		
	}
	
}
