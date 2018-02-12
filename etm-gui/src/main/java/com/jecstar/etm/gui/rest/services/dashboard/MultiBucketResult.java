package com.jecstar.etm.gui.rest.services.dashboard;

import com.jecstar.etm.domain.writer.json.JsonWriter;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.AggregationKey;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.AggregationValue;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.LongAggregationValue;

import java.util.*;
import java.util.Map.Entry;

public class MultiBucketResult {

    public enum SortOrder {ASC, DESC}

    private boolean addMissingKeys = false;
    private final Map<String, Map<AggregationKey, AggregationValue<?>>> values = new HashMap<>();
    private SortOrder sortOrder = SortOrder.ASC;
    private final Set<AggregationKey> keys = new HashSet<>();

    public void addValueToSerie(String seriesName, AggregationKey key, AggregationValue<?> value) {
        if (!this.values.containsKey(seriesName)) {
            this.values.put(seriesName, new HashMap<>());
        }
        Map<AggregationKey, AggregationValue<?>> map = this.values.get(seriesName);
        map.put(key, value);
        this.keys.add(key);
    }

    public MultiBucketResult setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    public MultiBucketResult setAddMissingKeys(boolean addMissingKeys) {
        this.addMissingKeys = addMissingKeys;
        return this;
    }

    public void appendAsArrayToJsonBuffer(JsonWriter jsonWriter, StringBuilder buffer, boolean firstElement) {
        if (!firstElement) {
            buffer.append(", ");
        }
        buffer.append("[");
        boolean firstSerie = true;
        for (Entry<String, Map<AggregationKey, AggregationValue<?>>> serie : this.values.entrySet()) {
            if (!firstSerie) {
                buffer.append(",");
            }
            int labelLength = 0;
            buffer.append("{");
            jsonWriter.addStringElementToJsonBuffer("key", serie.getKey(), buffer, true);
            buffer.append(",\"values\": [");
            boolean firstItem = true;
            List<AggregationKey> keys = this.addMissingKeys ? new ArrayList<>(this.keys) : new ArrayList<>(serie.getValue().keySet());
            Collections.sort(keys);
            if (SortOrder.DESC.equals(this.sortOrder)) {
                Collections.reverse(keys);
            }
            for (AggregationKey key : keys) {
                if (!firstItem) {
                    buffer.append(",");
                }
                if (key.getLength() > labelLength) {
                    labelLength = key.getLength();
                }
                AggregationValue<?> aggregationValue = serie.getValue().get(key);
                if (aggregationValue == null) {
                    if (!this.addMissingKeys) {
                        // Should never happen.
                        continue;
                    }
                    aggregationValue = new LongAggregationValue("?", 0L);
                }
                buffer.append("{");
                jsonWriter.addStringElementToJsonBuffer("label", key.getKeyAsString(), buffer, true);
                aggregationValue.appendValueToJsonBuffer(jsonWriter, buffer, false);
                buffer.append("}");
                firstItem = false;
            }
            buffer.append("]");
            buffer.append(", \"max_label_length\": ").append(labelLength);
            buffer.append("}");
            firstSerie = false;
        }
        buffer.append("]");
    }

}
