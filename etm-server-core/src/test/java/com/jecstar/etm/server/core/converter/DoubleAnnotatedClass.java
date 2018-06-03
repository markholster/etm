package com.jecstar.etm.server.core.converter;

import java.util.Objects;

class DoubleAnnotatedClass {

    @JsonField("first_value")
    private Double firstValue;

    @JsonField(value = "second_value", writeWhenNull = true)
    private Double secondValue;

    @JsonField("third_value")
    private double thirdValue;

    private Double getFirstValue() {
        return this.firstValue;
    }

    void setFirstValue(Double firstValue) {
        this.firstValue = firstValue;
    }

    private Double getSecondValue() {
        return this.secondValue;
    }

    void setSecondValue(Double secondValue) {
        this.secondValue = secondValue;
    }

    private double getThirdValue() {
        return this.thirdValue;
    }

    void setThirdValue(double thirdValue) {
        this.thirdValue = thirdValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DoubleAnnotatedClass) {
            DoubleAnnotatedClass other = (DoubleAnnotatedClass) obj;
            return Objects.equals(getFirstValue(), other.getFirstValue())
                    && Objects.equals(getSecondValue(), other.getSecondValue())
                    && getThirdValue() == other.getThirdValue();
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFirstValue(), getSecondValue(), getThirdValue());
    }
}
