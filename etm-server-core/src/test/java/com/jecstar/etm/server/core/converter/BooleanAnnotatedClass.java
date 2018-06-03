package com.jecstar.etm.server.core.converter;

import java.util.Objects;

class BooleanAnnotatedClass {

    @JsonField("first_value")
    private Boolean firstValue;

    @JsonField(value = "second_value", writeWhenNull = true)
    private Boolean secondValue;

    @JsonField("third_value")
    private boolean thirdValue;

    private Boolean getFirstValue() {
        return this.firstValue;
    }

    void setFirstValue(Boolean firstValue) {
        this.firstValue = firstValue;
    }

    private Boolean getSecondValue() {
        return this.secondValue;
    }

    void setSecondValue(Boolean secondValue) {
        this.secondValue = secondValue;
    }

    private boolean getThirdValue() {
        return this.thirdValue;
    }

    void setThirdValue(boolean thirdValue) {
        this.thirdValue = thirdValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BooleanAnnotatedClass) {
            BooleanAnnotatedClass other = (BooleanAnnotatedClass) obj;
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
