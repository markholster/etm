package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

import com.jecstar.etm.domain.writer.json.JsonWriter;

import java.text.Format;

public class LongAggregationValue extends AbstractAggregationValue<Long> {

    private static final Long DEFAULT_MISSING_VALUE = 0L;
    private final Long value;
    private Long missingValue = DEFAULT_MISSING_VALUE;

    public LongAggregationValue(String label, Long value) {
        super(label);
        this.value = value;
    }

    @Override
    public Long getValue() {
        if (hasValidValue()) {
            return this.value;
        }
        if (isValidValue(this.missingValue)) {
            return this.missingValue;
        }
        return null;
    }

    @Override
    public Long getMissingValue() {
        return this.missingValue;
    }

    @Override
    public void setMissingValue(Long missingValue) {
        this.missingValue = missingValue;
    }

    @Override
    public String getValueAsString(Format format) {
        if (hasValidValue()) {
            return format.format(getValue());
        }
        if (isValidValue(this.missingValue)) {
            return format.format(this.missingValue);
        }
        return "?";
    }

    @Override
    public void appendValueToJsonBuffer(JsonWriter jsonWriter, StringBuilder buffer, boolean firstElement) {
        jsonWriter.addLongElementToJsonBuffer("value", getValue(), buffer, firstElement);
    }

    @Override
    public boolean hasValidValue() {
        return isValidValue(this.value);
    }

    private boolean isValidValue(Long value) {
        return value != null;
    }

    @Override
    public boolean isEqualTo(int value) {
        if (hasValidValue()) {
            return this.value.compareTo((long) value) == 0;
        }
        return DEFAULT_MISSING_VALUE.compareTo((long) value) == 0;
    }

    @Override
    public boolean isLessThan(int value) {
        if (hasValidValue()) {
            return this.value.compareTo((long) value) < 0;
        }
        return DEFAULT_MISSING_VALUE.compareTo((long) value) < 0;
    }

    @Override
    public boolean isGreaterThan(int value) {
        if (hasValidValue()) {
            return this.value.compareTo((long) value) > 0;
        }
        return DEFAULT_MISSING_VALUE.compareTo((long) value) > 0;
    }
}
