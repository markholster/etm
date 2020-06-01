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

import java.util.concurrent.TimeUnit;

public interface MetricConverterTags {

    enum RateType {EVENTS, CALLS}

    default String getIdTag() {
        return "id";
    }

    default String getTimestampTag() {
        return "timestamp";
    }

    default String getNodeTag() {
        return "node";
    }

    default String getNameTag() {
        return "name";
    }

    default String getCountTag() {
        return "count";
    }

    default String getMinTag() {
        return "min";
    }

    default String getMaxTag() {
        return "max";
    }

    default String getMeanTag() {
        return "mean";
    }

    default String getStandardDeviationTag() {
        return "stddev";
    }

    default String getMedianTag() {
        return "median";
    }

    default String get75thPercentileTag() {
        return "75%%";
    }

    default String get95thPercentileTag() {
        return "95%%";
    }

    default String get98thPercentileTag() {
        return "98%%";
    }

    default String get99thPercentileTag() {
        return "99%%";
    }

    default String get999thPercentileTag() {
        return "99_9%%";
    }

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
