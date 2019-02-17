package com.jecstar.etm.server.core.converter;

import java.time.Instant;
import java.util.Objects;

class InstantAnnotatedClass {

    @JsonField("first_value")
    private Instant firstValue;

    @JsonField(value = "second_value", writeWhenNull = true)
    private Instant secondValue;

    private Instant getFirstValue() {
        return this.firstValue;
    }

    void setFirstValue(Instant firstValue) {
        this.firstValue = firstValue;
    }

    private Instant getSecondValue() {
        return this.secondValue;
    }

    void setSecondValue(Instant secondValue) {
        this.secondValue = secondValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InstantAnnotatedClass) {
            InstantAnnotatedClass other = (InstantAnnotatedClass) obj;
            return Objects.equals(getFirstValue(), other.getFirstValue())
                    && Objects.equals(getSecondValue(), other.getSecondValue());
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFirstValue(), getSecondValue());
    }
}
