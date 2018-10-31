package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

import com.jecstar.etm.domain.writer.json.JsonWriter;

import java.text.Format;

/**
 * Abstract superclass for all <code>AggregationValue</code> instances.
 *
 * @param <T> The type of the value.
 */
public abstract class AbstractAggregationValue<T> implements AggregationValue<T> {

    /**
     * The label that belongs to the value.
     */
    private final String label;

    /**
     * Boolean indicating this value is a percentage.
     */
    private boolean percentage;

    AbstractAggregationValue(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public void appendToJsonBuffer(JsonWriter jsonWriter, Format format, StringBuilder buffer, boolean firstElement) {
        jsonWriter.addStringElementToJsonBuffer("label", getLabel(), buffer, firstElement);
        appendValueToJsonBuffer(jsonWriter, buffer, false);
        jsonWriter.addStringElementToJsonBuffer("value_as_string", getValueAsString(format) + (isPercentage() ? "%" : ""), buffer, false);
    }

    @Override
    public AggregationValue<T> setPercentage(boolean percentage) {
        this.percentage = percentage;
        return this;
    }

    @Override
    public boolean isPercentage() {
        return this.percentage;
    }

}
