package com.jecstar.etm.server.core.converter;

import java.util.Objects;

class IntegerAnnotatedClass {

    @JsonField("first_value")
    private Integer firstValue;

    @JsonField(value = "second_value", writeWhenNull = true)
    private Integer secondValue;

    @JsonField("third_value")
    private int thirdValue;

    private Integer getFirstValue() {
        return this.firstValue;
    }

    void setFirstValue(Integer firstValue) {
        this.firstValue = firstValue;
    }

    private Integer getSecondValue() {
        return this.secondValue;
    }

    void setSecondValue(Integer secondValue) {
        this.secondValue = secondValue;
    }

    private int getThirdValue() {
        return this.thirdValue;
    }

    void setThirdValue(int thirdValue) {
        this.thirdValue = thirdValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IntegerAnnotatedClass) {
            IntegerAnnotatedClass other = (IntegerAnnotatedClass) obj;
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
