package com.jecstar.etm.launcher;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MetricConverterTagsJsonImpl implements MetricConverterTags {

	@Override
	public String getTimestampTag() {
		return "timestamp";
	}

	@Override
	public String getNodeTag() {
		return "node";
	}
	
	@Override
	public String getComponentTag() {
		return "component";
	}

	@Override
	public String getNameTag() {
		return "name";
	}

	@Override
	public String getCountTag() {
		return "count";
	}

	@Override
	public String getMinTag() {
		return "min";
	}
	
	@Override
	public String getMaxTag() {
		return "max";
	}

	@Override
	public String getMeanTag() {
		return "mean";
	}
	
	@Override
	public String getStandardDeviationTag() {
		return "stddev";
	}

	@Override
	public String getMedianTag() {
		return "median";
	}

	@Override
	public String get75thPercentileTag() {
		return "75%%";
	}
	
	@Override
	public String get95thPercentileTag() {
		return "95%%";
	}
	
	@Override
	public String get98thPercentileTag() {
		return "98%%";
	}
	
	@Override
	public String get99thPercentileTag() {
		return "99%%";
	}
	
	@Override
	public String get999thPercentileTag() {
		return "99_9%%";
	}

	@Override
	public String getMeanRateTag(RateType rateType, TimeUnit rateUnit) {
		return "mean-rate-" + rateType.name().toLowerCase() + "-per-" + calculateRateUnit(rateUnit);
	}
	
	@Override
	public String getOneMinuteRateTag(RateType rateType, TimeUnit rateUnit) {
		return "1-minute-rate-" + rateType.name().toLowerCase() + "-per-" + calculateRateUnit(rateUnit);
	}
	
	@Override
	public String getFiveMinuteRateTag(RateType rateType, TimeUnit rateUnit) {
		return "5-minute-rate-" + rateType.name().toLowerCase() + "-per-" + calculateRateUnit(rateUnit);
	}
	
	@Override
	public String getFifteenMinuteRateTag(RateType rateType, TimeUnit rateUnit) {
		return "15-minute-rate-" + rateType.name().toLowerCase() + "-per-" + calculateRateUnit(rateUnit);
	}

	@Override
	public String getMinDurationTag(TimeUnit durationUnit) {
		return getMinTag() + "-in-" + calculateDurationUnit(durationUnit);
	}
	
	@Override
	public String getMaxDurationTag(TimeUnit durationUnit) {
		return getMaxTag() + "-in-" + calculateDurationUnit(durationUnit);
	}

	@Override
	public String getMeanDurationTag(TimeUnit durationUnit) {
		return getMeanTag() + "-in-" + calculateDurationUnit(durationUnit);
	}

	@Override
	public String getStandardDeviationDurationTag(TimeUnit durationUnit) {
		return getStandardDeviationTag() + "-in-" + calculateDurationUnit(durationUnit);
	}

	@Override
	public String getMedianDurationTag(TimeUnit durationUnit) {
		return getMedianTag() + "-in-" + calculateDurationUnit(durationUnit);
	}

	@Override
	public String get75thPercentileDurationTag(TimeUnit durationUnit) {
		return get75thPercentileTag() + "-in-" + calculateDurationUnit(durationUnit);
	}

	@Override
	public String get95thPercentileDurationTag(TimeUnit durationUnit) {
		return get95thPercentileTag() + "-in-" + calculateDurationUnit(durationUnit);
	}

	@Override
	public String get98thPercentileDurationTag(TimeUnit durationUnit) {
		return get98thPercentileTag() + "-in-" + calculateDurationUnit(durationUnit);
	}

	@Override
	public String get99thPercentileDurationTag(TimeUnit durationUnit) {
		return get99thPercentileTag() + "-in-" + calculateDurationUnit(durationUnit);
	}

	@Override
	public String get999thPercentileDurationTag(TimeUnit durationUnit) {
		return get999thPercentileTag() + "-in-" + calculateDurationUnit(durationUnit);
	}
	
	private String calculateDurationUnit(TimeUnit unit) {
		return escapeToJson(unit.toString().toLowerCase(Locale.US));
	}
	
	private String calculateRateUnit(TimeUnit unit) {
		final String s = unit.toString().toLowerCase(Locale.US);
		return escapeToJson(s.substring(0, s.length() - 1));
	}
	
	private String escapeToJson(String value) {
		return value.replace("\"", "\\\"");
	}
}
