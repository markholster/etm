package com.jecstar.etm.processor.elastic;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.SortedMap;
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
import com.codahale.metrics.Timer;
import com.jecstar.etm.processor.metrics.MetricConverter;
import com.jecstar.etm.processor.metrics.MetricConverterJsonImpl;
import com.jecstar.etm.processor.metrics.MetricConverterTags;

public class MetricReporterElasticImpl extends ScheduledReporter {

	private static final TimeUnit rateUnit = TimeUnit.SECONDS;
	private static final TimeUnit durationUnit = TimeUnit.MILLISECONDS;
	private final Client elasticClient;
	private final String nodeName;
	
	private final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.YEAR, 4)
			.appendLiteral("-")
			.appendValue(ChronoField.MONTH_OF_YEAR, 2)
			.appendLiteral("-")
			.appendValue(ChronoField.DAY_OF_MONTH, 2).toFormatter().withZone(ZoneId.of("UTC"));
	private final MetricConverter<String> metricConverter = new MetricConverterJsonImpl();
	private final MetricConverterTags tags = this.metricConverter.getTags();

	public MetricReporterElasticImpl(MetricRegistry registry, String nodeName, Client elasticClient) {
		super(registry, nodeName, MetricFilter.ALL, rateUnit, durationUnit);
		this.elasticClient = elasticClient;
		this.nodeName = nodeName;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		StringBuilder buffer = new StringBuilder();
		Instant now = Instant.now();
		buffer.append("{");
		buffer.append("\"" + this.tags.getTimestampTag() + "\": " + now.toEpochMilli());
		appendNodeInfo(buffer);
		if (gauges != null && !gauges.isEmpty()) {
			buffer.append(", ");
			buffer.append(this.metricConverter.convertGauges(gauges));
		}
		if (counters != null && !counters.isEmpty()) {
			buffer.append(", ");
			buffer.append(this.metricConverter.convertCounters(counters));
		}
		if (histograms != null && !histograms.isEmpty()) {
			buffer.append(", ");
			buffer.append(this.metricConverter.convertHistograms(histograms));
		}
		if (meters != null && !meters.isEmpty()) {
			buffer.append(", ");
			buffer.append(this.metricConverter.convertMeters(meters, rateUnit));
		}
		if (timers != null && !timers.isEmpty()) {
			buffer.append(", ");
			buffer.append(this.metricConverter.convertTimers(timers, rateUnit, durationUnit));
		}
        buffer.append("}");
        this.elasticClient.prepareIndex(getElasticIndexName(now), "processor", "" + now.toEpochMilli())
        	.setConsistencyLevel(WriteConsistencyLevel.ONE)
        	.setSource(buffer.toString()).get();
	}
	
	private void appendNodeInfo(StringBuilder buffer) {
		buffer.append(", \"" + this.tags.getNodeTag() + "\": {");
		buffer.append("\"" + this.tags.getNameTag() + "\": \"" + escapeToJson(this.nodeName) + "\"");
		buffer.append("}");
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
	
	private String escapeToJson(String value) {
		return value.replace("\"", "\\\"");
	}
}
