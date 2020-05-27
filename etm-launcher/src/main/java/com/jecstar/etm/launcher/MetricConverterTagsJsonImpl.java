/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.launcher;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MetricConverterTagsJsonImpl implements MetricConverterTags {

    @Override
    public String getMeanRateTag(RateType rateType, TimeUnit rateUnit) {
        return "mean_rate_" + rateType.name().toLowerCase() + "_per_" + calculateRateUnit(rateUnit);
    }

    @Override
    public String getOneMinuteRateTag(RateType rateType, TimeUnit rateUnit) {
        return "1_minute_rate_" + rateType.name().toLowerCase() + "_per_" + calculateRateUnit(rateUnit);
    }

    @Override
    public String getFiveMinuteRateTag(RateType rateType, TimeUnit rateUnit) {
        return "5_minute_rate_" + rateType.name().toLowerCase() + "_per_" + calculateRateUnit(rateUnit);
    }

    @Override
    public String getFifteenMinuteRateTag(RateType rateType, TimeUnit rateUnit) {
        return "15_minute_rate_" + rateType.name().toLowerCase() + "_per_" + calculateRateUnit(rateUnit);
    }

    @Override
    public String getMinDurationTag(TimeUnit durationUnit) {
        return getMinTag() + "_in_" + calculateDurationUnit(durationUnit);
    }

    @Override
    public String getMaxDurationTag(TimeUnit durationUnit) {
        return getMaxTag() + "_in_" + calculateDurationUnit(durationUnit);
    }

    @Override
    public String getMeanDurationTag(TimeUnit durationUnit) {
        return getMeanTag() + "_in_" + calculateDurationUnit(durationUnit);
    }

    @Override
    public String getStandardDeviationDurationTag(TimeUnit durationUnit) {
        return getStandardDeviationTag() + "_in_" + calculateDurationUnit(durationUnit);
    }

    @Override
    public String getMedianDurationTag(TimeUnit durationUnit) {
        return getMedianTag() + "_in_" + calculateDurationUnit(durationUnit);
    }

    @Override
    public String get75thPercentileDurationTag(TimeUnit durationUnit) {
        return get75thPercentileTag() + "_in_" + calculateDurationUnit(durationUnit);
    }

    @Override
    public String get95thPercentileDurationTag(TimeUnit durationUnit) {
        return get95thPercentileTag() + "_in_" + calculateDurationUnit(durationUnit);
    }

    @Override
    public String get98thPercentileDurationTag(TimeUnit durationUnit) {
        return get98thPercentileTag() + "_in_" + calculateDurationUnit(durationUnit);
    }

    @Override
    public String get99thPercentileDurationTag(TimeUnit durationUnit) {
        return get99thPercentileTag() + "_in_" + calculateDurationUnit(durationUnit);
    }

    @Override
    public String get999thPercentileDurationTag(TimeUnit durationUnit) {
        return get999thPercentileTag() + "_in_" + calculateDurationUnit(durationUnit);
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
