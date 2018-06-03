package com.jecstar.etm.server.core.converter;

import java.time.ZonedDateTime;
import java.util.Objects;

class ZonedDateTimeAnnotatedClass {

    @JsonField("first_value")
    private ZonedDateTime firstValue;

    @JsonField(value = "second_value", writeWhenNull = true)
    private ZonedDateTime secondValue;

    private ZonedDateTime getFirstValue() {
        return this.firstValue;
    }

    void setFirstValue(ZonedDateTime firstValue) {
        this.firstValue = firstValue;
    }

    private ZonedDateTime getSecondValue() {
        return this.secondValue;
    }

    void setSecondValue(ZonedDateTime secondValue) {
        this.secondValue = secondValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ZonedDateTimeAnnotatedClass) {
            ZonedDateTimeAnnotatedClass other = (ZonedDateTimeAnnotatedClass) obj;
            return Objects.equals(getFirstValue() == null ? null : getFirstValue().toInstant(), other.getFirstValue() == null ? null : other.getFirstValue().toInstant())
                    && Objects.equals(getSecondValue() == null ? null : getSecondValue().toInstant(), other.getSecondValue() == null ? null : other.getSecondValue().toInstant());
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFirstValue(), getSecondValue());
    }
}
