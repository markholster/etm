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

    String getTimestampTag();

    String getNodeTag();

    String getNameTag();

    String getCountTag();

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
