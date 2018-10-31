package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

import com.jecstar.etm.domain.writer.json.JsonWriter;

import java.text.Format;
import java.time.Instant;

public class DateTimeAggregationKey implements AggregationKey {

    private final Instant instant;
    private final Format format;

    public DateTimeAggregationKey(Instant epochtime, Format format) {
        this.instant = epochtime;
        this.format = format;
    }

    @Override
    public int compareTo(AggregationKey o) {
        if (o instanceof DateTimeAggregationKey) {
            DateTimeAggregationKey other = (DateTimeAggregationKey) o;
            return this.instant.compareTo(other.instant);
        }
        return 0;
    }

    @Override
    public String getKeyAsString() {
        return this.format.format(this.instant);
    }

    @Override
    public String toJsonValue(JsonWriter jsonWriter) {
        return Long.toString(this.instant.toEpochMilli());
    }

    public Instant getInstant() {
        return this.instant;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DateTimeAggregationKey) {
            DateTimeAggregationKey other = (DateTimeAggregationKey) obj;
            return this.instant == other.instant;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.instant.hashCode();
    }
}
