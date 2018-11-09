package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.domain.writer.json.JsonWriter;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.AggregationKey;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.AggregationValue;
import com.jecstar.etm.gui.rest.services.dashboard.aggregation.DateTimeAggregationKey;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class MultiBucketResult {

    private final MultiBucketGraph graph;

    public enum SortOrder {ASC, DESC}

    private AggregationValue missingKeyValue;
    private final Map<String, Map<AggregationKey, AggregationValue<?>>> values = new HashMap<>();
    private SortOrder sortOrder = SortOrder.ASC;
    private final TreeSet<AggregationKey> keys = new TreeSet<>();
    private final JsonWriter jsonWriter = new JsonWriter();

    public MultiBucketResult(MultiBucketGraph graph) {
        this.graph = graph;
    }

    public void addValueToSerie(String seriesName, AggregationKey key, AggregationValue<?> value) {
        if (!this.values.containsKey(seriesName)) {
            this.values.put(seriesName, new HashMap<>());
        }
        Map<AggregationKey, AggregationValue<?>> map = this.values.get(seriesName);
        map.put(key, value);
        this.keys.add(key);
    }

    public MultiBucketResult setMissingKeyValue(AggregationValue missingKeyValue) {
        this.missingKeyValue = missingKeyValue;
        return this;
    }

    public void appendAsArrayToJsonBuffer(JsonWriter jsonWriter, StringBuilder buffer, boolean firstElement) {
        if (!firstElement) {
            buffer.append(", ");
        }
        buffer.append("{");
        if (!isDateTimeBucketList()) {
            // A dateTime bucket list must not return any categories. They are calculates by Highcharts based on pointStart and pointInterval
            writeCategories(jsonWriter, buffer);
            buffer.append(",");
        }
        writeXAxis(jsonWriter, buffer);
        buffer.append(",");
        writeYAxis(jsonWriter, buffer);
        buffer.append(",");
        writeSeries(jsonWriter, buffer);
        buffer.append("}");
    }

    private void writeCategories(JsonWriter jsonWriter, StringBuilder buffer) {
        buffer.append(jsonWriter.escapeToJson("categories", true) + ": [");
        buffer.append(
                this.keys.stream()
                        .sorted(SortOrder.ASC.equals(this.sortOrder) ? Comparator.naturalOrder() : Comparator.reverseOrder())
                        .map(k -> k.toJsonValue(jsonWriter))
                        .collect(Collectors.joining(","))
        );
        buffer.append("]");
    }

    private void writeXAxis(JsonWriter jsonWriter, StringBuilder buffer) {
        buffer.append(jsonWriter.escapeToJson("xAxis", true) + ": {");
        jsonWriter.addStringElementToJsonBuffer("type", isDateTimeBucketList() ? "datetime" : "linear", buffer, true);
        buffer.append("}");
    }

    private void writeYAxis(JsonWriter jsonWriter, StringBuilder buffer) {
        buffer.append(jsonWriter.escapeToJson("yAxis", true) + ": {");
        jsonWriter.addStringElementToJsonBuffer("format", this.graph == null ? null : this.graph.getAxes().getYAxis().getFormat(), buffer, true);
        buffer.append("}");
    }

    private void writeSeries(JsonWriter jsonWriter, StringBuilder buffer) {
        List<AggregationKey> keys = this.keys.stream()
                .sorted(SortOrder.ASC.equals(this.sortOrder) ? Comparator.naturalOrder() : Comparator.reverseOrder())
                .collect(Collectors.toList());
        buffer.append(jsonWriter.escapeToJson("series", true) + ": [");
        boolean firstSerie = true;
        for (Entry<String, Map<AggregationKey, AggregationValue<?>>> serie : this.values.entrySet()) {
            if (!firstSerie) {
                buffer.append(",");
            }
            buffer.append("{");
            jsonWriter.addStringElementToJsonBuffer("name", serie.getKey(), buffer, true);
            buffer.append(",\"data\": [");
            boolean firstItem = true;
            for (AggregationKey key : keys) {
                AggregationValue<?> aggregationValue = serie.getValue().get(key);
                if (aggregationValue == null && this.missingKeyValue == null) {
                    continue;
                }
                if (!firstItem) {
                    buffer.append(",");
                }
                buffer.append("{ \"x\": ");
                buffer.append(key.toJsonValue(this.jsonWriter));
                buffer.append(", \"y\":  ");
                if (aggregationValue != null) {
                    buffer.append(aggregationValue.getValue());
                } else if (this.missingKeyValue != null) {
                    buffer.append(this.missingKeyValue.getValue());
                }
                buffer.append("}");
                firstItem = false;
            }
            buffer.append("]");
            buffer.append("}");
            firstSerie = false;
        }
        buffer.append("]");
    }

    private boolean isDateTimeBucketList() {
        return this.keys.stream().allMatch(p -> p instanceof DateTimeAggregationKey);
    }
}
