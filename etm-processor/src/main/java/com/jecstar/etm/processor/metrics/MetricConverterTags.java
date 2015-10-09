package com.jecstar.etm.processor.metrics;

import java.util.concurrent.TimeUnit;

public interface MetricConverterTags {
	
	public enum RateType {EVENTS, CALLS};

	String getTimestampTag();

	String getNodeNameTag();

	String getGaugesTag();

	String getCountersTag();

	String getValuesTag();

	String getHistogramsTag();

	String getCountTag();

	String getMetersTag();

	String getTimersTag();

	String getNameTag();

	String getValueTag();

	String getMinTag();

	String getMaxTag();

	String getMeanTag();

	String getStandardDeviationTag();

	String getMedianTag();

	String get75thPercentileTag();

	String get95thPercentileTag();

	String get98thPercentileTag();

	String get99thPercentileTag();

	String get999thPercentileTag();

	String getMeanRateTag(RateType rateType, TimeUnit rateUnit);

	String getOneMinuteRateTag(RateType rateType, TimeUnit rateUnit);

	String getFiveMinuteRateTag(RateType rateType, TimeUnit rateUnit);

	String getFifteenMinuteRateTag(RateType rateType, TimeUnit rateUnit);

	String getMinDurationTag(TimeUnit durationUnit);

	String getMaxDurationTag(TimeUnit durationUnit);
	
	String getMeanDurationTag(TimeUnit durationUnit);

	String getStandardDeviationDurationTag(TimeUnit durationUnit);

	String getMedianDurationTag(TimeUnit durationUnit);

	String get75thPercentileDurationTag(TimeUnit durationUnit);

	String get95thPercentileDurationTag(TimeUnit durationUnit);

	String get98thPercentileDurationTag(TimeUnit durationUnit);

	String get99thPercentileDurationTag(TimeUnit durationUnit);

	String get999thPercentileDurationTag(TimeUnit durationUnit);


}
