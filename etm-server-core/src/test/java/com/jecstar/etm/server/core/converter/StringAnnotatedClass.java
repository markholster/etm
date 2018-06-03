package com.jecstar.etm.server.core.converter;

import java.util.Objects;

class StringAnnotatedClass {

    @JsonField("first_value")
    private String firstValue;

    @JsonField(value = "second_value", writeWhenNull = true)
    private String secondValue;

    private String getFirstValue() {
        return this.firstValue;
    }

    void setFirstValue(String firstValue) {
        this.firstValue = firstValue;
    }

    private String getSecondValue() {
        return this.secondValue;
    }

    void setSecondValue(String secondValue) {
        this.secondValue = secondValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StringAnnotatedClass) {
            StringAnnotatedClass other = (StringAnnotatedClass) obj;
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
