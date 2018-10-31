package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

import com.jecstar.etm.domain.writer.json.JsonWriter;

public class StringAggregationKey implements AggregationKey {

    private final String key;

    public StringAggregationKey(String key) {
        this.key = key;
    }

    @Override
    public int compareTo(AggregationKey o) {
        return this.key.compareTo(o.getKeyAsString());
    }

    @Override
    public String getKeyAsString() {
        return this.key;
    }

    @Override
    public String toJsonValue(JsonWriter jsonWriter) {
        return jsonWriter.escapeToJson(this.key, true);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StringAggregationKey) {
            StringAggregationKey other = (StringAggregationKey) obj;
            return this.key.equals(other.key);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

}
