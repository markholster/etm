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

package com.jecstar.etm.server.core.domain.aggregator.bucket;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.joda.time.DateTime;

import java.time.ZonedDateTime;

public class BucketKey {

    private final String jsonValue;

    public BucketKey(MultiBucketsAggregation.Bucket bucket, boolean keyAsString) {
        if (bucket.getKey() instanceof DateTime) {
            DateTime value = (DateTime) bucket.getKey();
            this.jsonValue = Long.toString(value.getMillis());
        } else if (bucket.getKey() instanceof ZonedDateTime) {
            ZonedDateTime value = (ZonedDateTime) bucket.getKey();
            this.jsonValue = Long.toString(value.toInstant().toEpochMilli());
        } else if (bucket.getKey() instanceof Double) {
            Double value = (Double) bucket.getKey();
            this.jsonValue = keyAsString ? JsonBuilder.escapeToJson(value.toString(), true) : value.toString();
        } else if (bucket.getKey() instanceof Long) {
            Long value = (Long) bucket.getKey();
            this.jsonValue = keyAsString ? JsonBuilder.escapeToJson(value.toString(), true) : value.toString();
        } else {
            this.jsonValue = JsonBuilder.escapeToJson(bucket.getKeyAsString(), true);
        }
    }

    /**
     * Gives the key in escaped json format.
     *
     * @return The key in escaped json format.
     */
    public String getJsonValue() {
        return this.jsonValue;
    }
}
