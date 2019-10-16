package com.jecstar.etm.server.core.domain.aggregator.bucket;

import com.jecstar.etm.domain.writer.json.JsonWriter;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.joda.time.DateTime;

import java.time.ZonedDateTime;

public class BucketKey {

    private static final JsonWriter jsonWriter = new JsonWriter();

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
            this.jsonValue = keyAsString ? jsonWriter.escapeToJson(value.toString(), true) : value.toString();
        } else if (bucket.getKey() instanceof Long) {
            Long value = (Long) bucket.getKey();
            this.jsonValue = keyAsString ? jsonWriter.escapeToJson(value.toString(), true) : value.toString();
        } else {
            this.jsonValue = jsonWriter.escapeToJson(bucket.getKeyAsString(), true);
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
